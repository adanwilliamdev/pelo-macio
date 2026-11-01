package com.pelomacio.service;

import com.pelomacio.dto.*;
import com.pelomacio.exception.NegocioException;
import com.pelomacio.exception.RecursoNaoEncontradoException;
import com.pelomacio.model.Loja;
import com.pelomacio.model.Pet.Porte;
import com.pelomacio.model.Profissional;
import com.pelomacio.model.Servico;
import com.pelomacio.model.Usuario;
import com.pelomacio.repository.LojaRepository;
import com.pelomacio.repository.ProfissionalRepository;
import com.pelomacio.repository.ServicoRepository;
import com.pelomacio.repository.UsuarioRepository;
import com.pelomacio.security.JwtService;
import com.pelomacio.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Cadastro de lojas novas (onboarding) e configuração da loja atual. */
@Service
public class LojaService {

    private static final Logger log = LoggerFactory.getLogger(LojaService.class);

    private final LojaRepository lojas;
    private final UsuarioRepository usuarios;
    private final ProfissionalRepository profissionais;
    private final ServicoRepository servicos;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final TransactionTemplate tx;

    public LojaService(LojaRepository lojas, UsuarioRepository usuarios, ProfissionalRepository profissionais,
                       ServicoRepository servicos, PasswordEncoder encoder, JwtService jwt, TransactionTemplate tx) {
        this.lojas = lojas;
        this.usuarios = usuarios;
        this.profissionais = profissionais;
        this.servicos = servicos;
        this.encoder = encoder;
        this.jwt = jwt;
        this.tx = tx;
    }

    /**
     * Cria a loja, o administrador e um começo utilizável (o próprio administrador como profissional e três
     * serviços de exemplo), para a página pública já funcionar. Devolve o login pronto.
     *
     * <p>São duas transações de propósito: a loja e o usuário são globais; o restante é dado da loja e precisa
     * ser gravado com a loja definida no contexto (o Hibernate fixa a loja quando abre a sessão).
     */
    public LoginResponse cadastrar(CadastroLojaRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
            throw new NegocioException("Já existe uma conta com este e-mail.", HttpStatus.CONFLICT);
        }
        Usuario[] admin = new Usuario[1];
        Loja loja = tx.execute(s -> {
            Loja nova = lojas.save(Loja.builder().nome(req.nomeLoja().trim()).slug(slugLivre(req.nomeLoja())).build());
            admin[0] = usuarios.save(Usuario.builder().lojaId(nova.getId()).nome(req.nome().trim()).email(email)
                    .senha(encoder.encode(req.senha())).perfis(Set.of(Usuario.Perfil.ADMINISTRADOR)).build());
            return nova;
        });
        try {
            TenantContext.executarComo(loja.getId(), () -> tx.executeWithoutResult(s -> criarPadroes(admin[0].getId())));
        } catch (RuntimeException e) {
            log.error("Falha ao preparar a loja {}; desfazendo o cadastro", loja.getSlug(), e);
            usuarios.deleteById(admin[0].getId());
            lojas.deleteById(loja.getId());
            throw e;
        }
        return LoginResponse.de(jwt.gerar(admin[0]), admin[0], loja);
    }

    private void criarPadroes(Long adminId) {
        Usuario admin = usuarios.findById(adminId).orElseThrow();
        profissionais.save(Profissional.builder().nome(admin.getNome()).especialidade("Banho e tosa")
                .usuario(admin).build());
        servicos.saveAll(List.of(
                servico("Banho", "Banho", 60, "50", 75, "70", 90, "90"),
                servico("Tosa higiênica", "Tosa", 30, "35", 40, "45", 50, "60"),
                servico("Banho e tosa", "Banho", 90, "80", 110, "110", 130, "140")));
    }

    private static Servico servico(String nome, String categoria, int dp, String pp, int dm, String pm, int dg, String pg) {
        return Servico.builder().nome(nome).categoria(categoria)
                .duracaoMinutos(dp).preco(new BigDecimal(pp))
                .duracaoMedio(dm).precoMedio(new BigDecimal(pm))
                .duracaoGrande(dg).precoGrande(new BigDecimal(pg)).build();
    }

    // ───────────── configuração da loja atual ─────────────

    public LojaResponse atual() {
        return LojaResponse.de(buscarAtual());
    }

    public LojaResponse atualizar(LojaAtualizarRequest req) {
        if (!req.abertura().isBefore(req.fechamento())) {
            throw new NegocioException("O horário de abertura deve ser antes do de fechamento.");
        }
        Loja loja = buscarAtual();
        loja.setNome(req.nome().trim());
        loja.setTelefone(req.telefone() == null || req.telefone().isBlank() ? null : req.telefone().trim());
        loja.setAbertura(req.abertura());
        loja.setFechamento(req.fechamento());
        loja.setIntervaloMinutos(req.intervaloMinutos());
        loja.setDiasFuncionamento(req.diasFuncionamento().stream().distinct().sorted()
                .map(String::valueOf).collect(Collectors.joining(",")));
        loja.setAgendamentoOnline(req.agendamentoOnline());
        return LojaResponse.de(lojas.save(loja));
    }

    private Loja buscarAtual() {
        Long id = TenantContext.exigir();
        return lojas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Loja", id));
    }

    /** Nome da loja em URL: sem acentos, minúsculo, hífens; se já existir, acrescenta -2, -3... */
    String slugLivre(String nome) {
        String base = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (base.length() > 40) base = base.substring(0, 40).replaceAll("-+$", "");
        if (base.isEmpty()) base = "loja";
        String slug = base;
        for (int i = 2; lojas.existsBySlug(slug); i++) slug = base + "-" + i;
        return slug;
    }
}
