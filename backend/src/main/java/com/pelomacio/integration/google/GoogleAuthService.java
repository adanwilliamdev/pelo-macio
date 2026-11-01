package com.pelomacio.integration.google;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.pelomacio.exception.NegocioException;
import com.pelomacio.model.GoogleConexao;
import com.pelomacio.model.Profissional;
import com.pelomacio.repository.GoogleConexaoRepository;
import com.pelomacio.security.TokenCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fluxo OAuth2 (authorization code, acesso offline) de cada profissional com a própria conta Google.
 * O refresh token fica cifrado no banco; os access tokens vivem só em memória.
 */
@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    static final HttpTransport TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON = GsonFactory.getDefaultInstance();

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String ESCOPO_AGENDA = "https://www.googleapis.com/auth/calendar.events";
    private static final List<String> ESCOPOS = List.of("openid", "email", ESCOPO_AGENDA);

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final GoogleConexaoRepository conexoes;
    private final TokenCipher cipher;
    private final Map<Long, Credential> credenciais = new ConcurrentHashMap<>();

    public GoogleAuthService(@Value("${pelomacio.google.client-id:}") String clientId,
                             @Value("${pelomacio.google.client-secret:}") String clientSecret,
                             @Value("${pelomacio.google.redirect-uri:}") String redirectUri,
                             GoogleConexaoRepository conexoes, TokenCipher cipher) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.conexoes = conexoes;
        this.cipher = cipher;
    }

    /** False enquanto GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET não forem definidos. */
    public boolean configurado() {
        return preenchido(clientId) && preenchido(clientSecret);
    }

    public String redirectUri() {
        return redirectUri;
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank() && !valor.startsWith("SEU_");
    }

    /** URL da tela de consentimento do Google. {@code state} deve vir de {@code JwtService.gerarEstadoOAuth}. */
    public String urlAutorizacao(String state) {
        GoogleAuthorizationCodeRequestUrl url = new GoogleAuthorizationCodeRequestUrl(clientId, redirectUri, ESCOPOS);
        url.setAccessType("offline");   // pede refresh token
        url.setState(state);
        url.set("prompt", "consent");   // garante o refresh token mesmo se já houve consentimento antes
        return url.build();
    }

    /** Troca o código devolvido pelo Google por tokens e grava (ou atualiza) a conexão da profissional. */
    @Transactional
    public GoogleConexao conectar(Profissional profissional, String code) {
        GoogleTokenResponse resposta;
        try {
            resposta = new GoogleAuthorizationCodeTokenRequest(
                    TRANSPORT, JSON, TOKEN_URL, clientId, clientSecret, code, redirectUri).execute();
        } catch (IOException e) {
            log.warn("Google OAuth: falha ao trocar o código por tokens: {}", e.getMessage());
            throw new NegocioException("Não foi possível concluir a conexão com o Google. Tente novamente.",
                    HttpStatus.BAD_GATEWAY);
        }

        // Na tela de consentimento a pessoa pode desmarcar a permissão da agenda.
        String escopos = resposta.getScope();
        if (escopos == null || !escopos.contains("calendar.events")) {
            throw new NegocioException("Para sincronizar, permita o acesso à agenda do Google na tela de autorização.");
        }

        Optional<GoogleConexao> existente = conexoes.findByProfissionalId(profissional.getId());
        String refresh = resposta.getRefreshToken();
        if (refresh == null) {
            if (existente.isEmpty()) {
                throw new NegocioException("O Google não enviou o acesso contínuo. Remova o Pelo Macio em "
                        + "myaccount.google.com/permissions e conecte novamente.");
            }
            refresh = cipher.decifrar(existente.get().getRefreshToken());
        }

        GoogleConexao conexao = existente.orElseGet(GoogleConexao::new);
        conexao.setProfissional(profissional);
        conexao.setEmail(emailDe(resposta));
        conexao.setRefreshToken(cipher.cifrar(refresh));
        conexao.setReautorizar(false);
        conexao.setConectadoEm(LocalDateTime.now());
        GoogleConexao salva = conexoes.save(conexao);
        credenciais.remove(profissional.getId());
        return salva;
    }

    /** Revoga o acesso no Google (melhor esforço) e apaga a conexão. Eventos já criados permanecem na agenda. */
    @Transactional
    public void desconectar(Profissional profissional) {
        conexoes.findByProfissionalId(profissional.getId()).ifPresent(c -> {
            try {
                revogar(cipher.decifrar(c.getRefreshToken()));
            } catch (Exception e) {
                log.warn("Google OAuth: não foi possível revogar o token ({})", e.getMessage());
            }
            conexoes.delete(c);
        });
        credenciais.remove(profissional.getId());
    }

    /** Conexão utilizável (existe e não precisa de nova autorização). */
    public Optional<GoogleConexao> conexaoAtiva(Profissional profissional) {
        if (profissional == null || profissional.getId() == null) return Optional.empty();
        return conexoes.findByProfissionalId(profissional.getId())
                .filter(c -> !Boolean.TRUE.equals(c.getReautorizar()));
    }

    @Transactional
    public void marcarReautorizar(GoogleConexao conexao) {
        conexao.setReautorizar(true);
        conexoes.save(conexao);
        credenciais.remove(conexao.getProfissional().getId());
    }

    /**
     * Credencial que renova o access token sozinha a partir do refresh token.
     * @throws IllegalStateException se o refresh token guardado não puder ser decifrado.
     */
    Credential credencial(GoogleConexao conexao) {
        Long chave = conexao.getProfissional().getId();
        Credential existente = credenciais.get(chave);
        if (existente != null) return existente;

        Credential nova = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(TRANSPORT)
                .setJsonFactory(JSON)
                .setTokenServerUrl(new GenericUrl(TOKEN_URL))
                .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                .build();
        nova.setRefreshToken(cipher.decifrar(conexao.getRefreshToken()));
        credenciais.put(chave, nova);
        return nova;
    }

    private static String emailDe(GoogleTokenResponse resposta) {
        try {
            return resposta.parseIdToken().getPayload().getEmail();
        } catch (Exception e) {
            return null; // informativo apenas
        }
    }

    private void revogar(String token) {
        RestClient.create().post().uri(REVOKE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("token=" + URLEncoder.encode(token, StandardCharsets.UTF_8))
                .retrieve().toBodilessEntity();
    }
}
