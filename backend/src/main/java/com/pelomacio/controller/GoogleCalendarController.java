package com.pelomacio.controller;

import com.pelomacio.dto.GoogleConexaoResponse;
import com.pelomacio.dto.GoogleStatusResponse;
import com.pelomacio.dto.SincronizacaoResultado;
import com.pelomacio.exception.NegocioException;
import com.pelomacio.exception.RecursoNaoEncontradoException;
import com.pelomacio.integration.google.GoogleAuthService;
import com.pelomacio.integration.google.GoogleCalendarService;
import com.pelomacio.model.GoogleConexao;
import com.pelomacio.model.Profissional;
import com.pelomacio.repository.GoogleConexaoRepository;
import com.pelomacio.repository.ProfissionalRepository;
import com.pelomacio.security.JwtService;
import com.pelomacio.tenant.TenantContext;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Conexão dos profissionais com o Google Calendar.
 * O administrador gerencia todos; o profissional gerencia apenas o próprio.
 */
@RestController
@RequestMapping("/api/google")
public class GoogleCalendarController {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarController.class);
    private static final int LIMITE_NA_CONEXAO = 50;
    private static final int LIMITE_MANUAL = 100;

    private final GoogleAuthService auth;
    private final GoogleCalendarService calendar;
    private final GoogleConexaoRepository conexoes;
    private final ProfissionalRepository profissionais;
    private final JwtService jwt;
    private final String frontendUrl;

    public GoogleCalendarController(GoogleAuthService auth, GoogleCalendarService calendar,
                                    GoogleConexaoRepository conexoes, ProfissionalRepository profissionais,
                                    JwtService jwt, @Value("${pelomacio.frontend.url}") String frontendUrl) {
        this.auth = auth;
        this.calendar = calendar;
        this.conexoes = conexoes;
        this.profissionais = profissionais;
        this.jwt = jwt;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/status")
    public GoogleStatusResponse status(Authentication authentication) {
        List<Profissional> visiveis = ehAdmin(authentication)
                ? profissionais.findAll(Sort.by("nome")).stream().filter(p -> !Boolean.FALSE.equals(p.getAtivo())).toList()
                : profissionais.findByUsuarioEmailIgnoreCase(authentication.getName()).map(List::of).orElse(List.of());

        Map<Long, GoogleConexao> porProfissional = new HashMap<>();
        List<Long> ids = visiveis.stream().map(Profissional::getId).toList();
        for (GoogleConexao c : conexoes.findByProfissionalIdIn(ids)) porProfissional.put(c.getProfissional().getId(), c);

        List<GoogleConexaoResponse> lista = visiveis.stream().map(p -> {
            GoogleConexao c = porProfissional.get(p.getId());
            String calendarId = p.getGoogleCalendarId() == null || p.getGoogleCalendarId().isBlank()
                    ? "primary" : p.getGoogleCalendarId().trim();
            return new GoogleConexaoResponse(p.getId(), p.getNome(),
                    c != null && !Boolean.TRUE.equals(c.getReautorizar()),
                    c != null && Boolean.TRUE.equals(c.getReautorizar()),
                    c != null ? c.getEmail() : null, c != null ? c.getConectadoEm() : null, calendarId);
        }).toList();
        return new GoogleStatusResponse(auth.configurado(), auth.redirectUri(), lista);
    }

    /** Devolve a URL do Google para o front redirecionar o navegador (o token Bearer não segue em redirects). */
    @GetMapping("/profissionais/{id}/autorizacao")
    public Map<String, String> autorizacao(@PathVariable Long id, Authentication authentication) {
        if (!auth.configurado()) {
            throw new NegocioException("A integração com o Google ainda não foi configurada no servidor.");
        }
        Profissional p = autorizar(id, authentication);
        return Map.of("url", auth.urlAutorizacao(jwt.gerarEstadoOAuth(p.getId(), TenantContext.exigir())));
    }

    /** Destino do redirecionamento do Google. Sem Authorization: a identidade vem do state assinado. */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error) {
        if (state == null) return voltar("invalido");
        JwtService.EstadoOAuth estado;
        try {
            estado = jwt.validarEstadoOAuth(state);
        } catch (JwtException | IllegalArgumentException e) {
            return voltar("invalido");
        }
        if (error != null || code == null) return voltar("negado"); // ex.: a pessoa clicou em "Cancelar"

        // o callback chega sem login: a loja vem do state assinado
        return TenantContext.executarComo(estado.lojaId(), () -> concluirConexao(estado.profissionalId(), code));
    }

    private ResponseEntity<Void> concluirConexao(Long profissionalId, String code) {
        Profissional profissional = profissionais.findById(profissionalId).orElse(null);
        if (profissional == null) return voltar("invalido");
        try {
            auth.conectar(profissional, code);
        } catch (NegocioException e) {
            log.warn("Conexão Google de {} não concluída: {}", profissional.getNome(), e.getMessage());
            return voltar(e.getStatus() == HttpStatus.BAD_GATEWAY ? "falha" : "permissao");
        }
        try {
            calendar.sincronizarPendentes(profissional, LIMITE_NA_CONEXAO);
        } catch (Exception e) {
            log.warn("Sincronização inicial de {} falhou: {}", profissional.getNome(), e.getMessage());
        }
        return voltar("ok");
    }

    /** POST (e não DELETE) para que o próprio profissional possa desconectar: DELETE é só do administrador. */
    @PostMapping("/profissionais/{id}/desconectar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desconectar(@PathVariable Long id, Authentication authentication) {
        auth.desconectar(autorizar(id, authentication));
    }

    @PostMapping("/profissionais/{id}/sincronizar")
    public SincronizacaoResultado sincronizar(@PathVariable Long id, Authentication authentication) {
        Profissional p = autorizar(id, authentication);
        if (!calendar.conectado(p)) {
            throw new NegocioException("Este profissional não está conectado ao Google.");
        }
        return calendar.sincronizarPendentes(p, LIMITE_MANUAL);
    }

    private Profissional autorizar(Long id, Authentication authentication) {
        Profissional p = profissionais.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", id));
        if (ehAdmin(authentication)) return p;
        boolean propria = p.getUsuario() != null && p.getUsuario().getEmail().equalsIgnoreCase(authentication.getName());
        if (!propria) {
            throw new NegocioException("Você só pode gerenciar a sua própria agenda do Google.", HttpStatus.FORBIDDEN);
        }
        return p;
    }

    private static boolean ehAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    private ResponseEntity<Void> voltar(String resultado) {
        URI destino = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/google-calendar").queryParam("google", resultado).build().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(destino).build();
    }
}
