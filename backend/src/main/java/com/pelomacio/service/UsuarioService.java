package com.pelomacio.service;

import com.pelomacio.dto.*;
import com.pelomacio.exception.NegocioException;
import com.pelomacio.exception.RecursoNaoEncontradoException;
import com.pelomacio.model.Cliente;
import com.pelomacio.model.Profissional;
import com.pelomacio.model.Usuario;
import com.pelomacio.model.Usuario.Perfil;
import com.pelomacio.repository.ClienteRepository;
import com.pelomacio.repository.ProfissionalRepository;
import com.pelomacio.repository.UsuarioRepository;
import com.pelomacio.tenant.TenantContext;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Cadastro de usuários, perfis de acesso e vínculos com profissionais/clientes. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final ProfissionalRepository profissionais;
    private final ClienteRepository clientes;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, ProfissionalRepository profissionais,
                          ClienteRepository clientes, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.profissionais = profissionais;
        this.clientes = clientes;
        this.encoder = encoder;
    }

    // ───────────── administração ─────────────

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        Map<Long, Profissional> profissionaisPorUsuario = new HashMap<>();
        for (Profissional p : profissionais.findAll()) {
            if (p.getUsuario() != null) profissionaisPorUsuario.put(p.getUsuario().getId(), p);
        }
        Map<Long, Cliente> clientesPorUsuario = new HashMap<>();
        for (Cliente c : clientes.findAll()) {
            if (c.getUsuario() != null) clientesPorUsuario.put(c.getUsuario().getId(), c);
        }
        return usuarios.findByLojaId(TenantContext.exigir(), Sort.by("nome")).stream()
                .map(u -> UsuarioResponse.de(u, profissionaisPorUsuario.get(u.getId()), clientesPorUsuario.get(u.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return resposta(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioCriarRequest req) {
        String email = normalizarEmail(req.email());
        garantirEmailLivre(email, null);
        Set<Perfil> perfis = new HashSet<>(req.perfis());

        Usuario u = usuarios.save(Usuario.builder()
                .lojaId(TenantContext.exigir())
                .nome(req.nome().trim())
                .email(email)
                .telefone(limpar(req.telefone()))
                .senha(encoder.encode(req.senha()))
                .perfis(perfis)
                .ativo(req.ativo() == null || req.ativo())
                .build());
        aplicarVinculos(u, perfis, req.profissionalId(), req.clienteId());
        return resposta(u);
    }

    /**
     * @param emailSolicitante e-mail de quem está editando (vem do token), usado para impedir que a
     *                         administrador se bloqueie fora do sistema.
     */
    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioAtualizarRequest req, String emailSolicitante) {
        Usuario u = buscarEntidade(id);
        String email = normalizarEmail(req.email());
        garantirEmailLivre(email, id);

        Set<Perfil> perfis = new HashSet<>(req.perfis());
        boolean ativo = req.ativo() == null || req.ativo();
        boolean proprio = emailSolicitante != null && u.getEmail().equalsIgnoreCase(emailSolicitante);
        boolean eraAdminAtiva = Boolean.TRUE.equals(u.getAtivo()) && u.getPerfis().contains(Perfil.ADMINISTRADOR);
        boolean seraAdminAtiva = ativo && perfis.contains(Perfil.ADMINISTRADOR);

        if (proprio && !ativo) {
            throw new NegocioException("Você não pode desativar a própria conta.");
        }
        if (proprio && u.getPerfis().contains(Perfil.ADMINISTRADOR) && !perfis.contains(Perfil.ADMINISTRADOR)) {
            throw new NegocioException("Você não pode remover o seu próprio perfil de administrador.");
        }
        if (eraAdminAtiva && !seraAdminAtiva && usuarios.contarAtivosComPerfil(Perfil.ADMINISTRADOR, TenantContext.exigir()) <= 1) {
            throw new NegocioException("Deve existir ao menos um administrador ativo.");
        }

        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setTelefone(limpar(req.telefone()));
        u.setAtivo(ativo);
        u.setPerfis(perfis);
        usuarios.save(u);
        aplicarVinculos(u, perfis, req.profissionalId(), req.clienteId());
        return resposta(u);
    }

    @Transactional
    public void redefinirSenha(Long id, String novaSenha) {
        Usuario u = buscarEntidade(id);
        u.setSenha(encoder.encode(novaSenha));
        usuarios.save(u);
    }

    @Transactional(readOnly = true)
    public OpcoesVinculo opcoesVinculo() {
        List<OpcoesVinculo.Opcao> profs = profissionais.findAll(Sort.by("nome")).stream()
                .map(p -> new OpcoesVinculo.Opcao(p.getId(), p.getNome(), p.getUsuario() != null ? p.getUsuario().getId() : null))
                .toList();
        List<OpcoesVinculo.Opcao> cls = clientes.findAll(Sort.by("nome")).stream()
                .map(c -> new OpcoesVinculo.Opcao(c.getId(), c.getNome(), c.getUsuario() != null ? c.getUsuario().getId() : null))
                .toList();
        return new OpcoesVinculo(profs, cls);
    }

    // ───────────── "Minha conta" ─────────────

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String email) {
        return resposta(buscarPorEmail(email));
    }

    @Transactional
    public UsuarioResponse atualizarPerfil(String email, PerfilRequest req) {
        Usuario u = buscarPorEmail(email);
        u.setNome(req.nome().trim());
        u.setTelefone(limpar(req.telefone()));
        usuarios.save(u);
        return resposta(u);
    }

    @Transactional
    public void alterarSenha(String email, AlterarSenhaRequest req) {
        Usuario u = buscarPorEmail(email);
        // 422 (e não 401): o front trata 401 como sessão expirada e desloga.
        if (!encoder.matches(req.senhaAtual(), u.getSenha())) {
            throw new NegocioException("Senha atual incorreta.");
        }
        u.setSenha(encoder.encode(req.novaSenha()));
        usuarios.save(u);
    }

    // ───────────── internos ─────────────

    /** Usuários não passam pelo filtro automático de loja, então a checagem é explícita: outra loja = não existe. */
    private Usuario buscarEntidade(Long id) {
        return usuarios.findById(id)
                .filter(u -> u.getLojaId().equals(TenantContext.exigir()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private Usuario buscarPorEmail(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NegocioException("Usuário não encontrado.", HttpStatus.UNAUTHORIZED));
    }

    private UsuarioResponse resposta(Usuario u) {
        return UsuarioResponse.de(u, profissionais.findByUsuarioId(u.getId()).orElse(null),
                clientes.findByUsuarioId(u.getId()).orElse(null));
    }

    private void garantirEmailLivre(String email, Long idAtual) {
        Optional<Usuario> existente = usuarios.findByEmailIgnoreCase(email);
        if (existente.isPresent() && !existente.get().getId().equals(idAtual)) {
            throw new NegocioException("Já existe um usuário com este e-mail.", HttpStatus.CONFLICT);
        }
    }

    private void aplicarVinculos(Usuario u, Set<Perfil> perfis, Long profissionalId, Long clienteId) {
        if (profissionalId != null && !perfis.contains(Perfil.PROFISSIONAL)) {
            throw new NegocioException("Para vincular um profissional, marque o perfil Profissional.");
        }
        if (clienteId != null && !perfis.contains(Perfil.CLIENTE)) {
            throw new NegocioException("Para vincular um cliente, marque o perfil Cliente.");
        }
        vincularProfissional(u, profissionalId);
        vincularCliente(u, clienteId);
    }

    private void vincularProfissional(Usuario u, Long profissionalId) {
        Optional<Profissional> atual = profissionais.findByUsuarioId(u.getId());
        if (profissionalId == null) {
            atual.ifPresent(p -> desvincular(p));
            return;
        }
        if (atual.isPresent() && atual.get().getId().equals(profissionalId)) return;
        Profissional alvo = profissionais.findById(profissionalId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", profissionalId));
        if (alvo.getUsuario() != null) {
            throw new NegocioException("Este profissional já está vinculado a outro usuário.");
        }
        atual.ifPresent(p -> desvincular(p));
        alvo.setUsuario(u);
        profissionais.save(alvo);
    }

    private void vincularCliente(Usuario u, Long clienteId) {
        Optional<Cliente> atual = clientes.findByUsuarioId(u.getId());
        if (clienteId == null) {
            atual.ifPresent(c -> desvincular(c));
            return;
        }
        if (atual.isPresent() && atual.get().getId().equals(clienteId)) return;
        Cliente alvo = clientes.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));
        if (alvo.getUsuario() != null) {
            throw new NegocioException("Este cliente já está vinculado a outro usuário.");
        }
        atual.ifPresent(c -> desvincular(c));
        alvo.setUsuario(u);
        clientes.save(alvo);
    }

    // saveAndFlush: libera o vínculo antigo antes do novo, por causa do índice único em usuario_id.
    private void desvincular(Profissional p) {
        p.setUsuario(null);
        profissionais.saveAndFlush(p);
    }

    private void desvincular(Cliente c) {
        c.setUsuario(null);
        clientes.saveAndFlush(c);
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
