package com.pelomacio.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.pelomacio.dto.AlterarSenhaRequest;
import com.pelomacio.dto.UsuarioAtualizarRequest;
import com.pelomacio.dto.UsuarioCriarRequest;
import com.pelomacio.dto.UsuarioResponse;
import com.pelomacio.exception.NegocioException;
import com.pelomacio.model.Profissional;
import com.pelomacio.model.Usuario;
import com.pelomacio.model.Usuario.Perfil;
import com.pelomacio.repository.ClienteRepository;
import com.pelomacio.repository.ProfissionalRepository;
import com.pelomacio.repository.UsuarioRepository;
import com.pelomacio.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class UsuarioServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ProfissionalRepository profissionais = mock(ProfissionalRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    private final UsuarioService service = new UsuarioService(usuarios, profissionais, clientes, encoder);

    @BeforeEach
    void entrarNaLoja() {
        TenantContext.definir(1L);
    }

    @AfterEach
    void sairDaLoja() {
        TenantContext.limpar();
    }

    private static Usuario admin(long id, String email) {
        return Usuario.builder().lojaId(1L).id(id).nome("Admin " + id).email(email).senha("hash")
                .perfis(new HashSet<>(Set.of(Perfil.ADMINISTRADOR))).ativo(true).build();
    }

    private void salvarAtribuindoId(long id) {
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            if (u.getId() == null) u.setId(id);
            return u;
        });
    }

    @Test
    void criaUsuarioComSenhaCriptografadaEEmailNormalizado() {
        when(encoder.encode("senha1234")).thenReturn("HASH");
        salvarAtribuindoId(5L);

        UsuarioResponse resp = service.criar(new UsuarioCriarRequest(
                " Ana ", "  Ana@PetShop.COM ", null, "senha1234", Set.of(Perfil.PROFISSIONAL), true, null, null));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(salvo.capture());
        assertEquals("HASH", salvo.getValue().getSenha());
        assertEquals("ana@petshop.com", resp.email());
        assertEquals("Ana", resp.nome());
        assertTrue(resp.ativo());
    }

    @Test
    void rejeitaEmailJaCadastrado() {
        when(usuarios.findByEmailIgnoreCase("ana@petshop.com")).thenReturn(Optional.of(admin(1L, "ana@petshop.com")));

        assertThrows(NegocioException.class, () -> service.criar(new UsuarioCriarRequest(
                "Ana", "ana@petshop.com", null, "senha1234", Set.of(Perfil.PROFISSIONAL), true, null, null)));
        verify(usuarios, never()).save(any());
    }

    @Test
    void naoVinculaProfissionalJaVinculadaAOutroUsuario() {
        salvarAtribuindoId(5L);
        Profissional ocupada = Profissional.builder().id(7L).nome("Bia").usuario(admin(99L, "x@x.com")).build();
        when(profissionais.findById(7L)).thenReturn(Optional.of(ocupada));

        assertThrows(NegocioException.class, () -> service.criar(new UsuarioCriarRequest(
                "Ana", "ana@petshop.com", null, "senha1234", Set.of(Perfil.PROFISSIONAL), true, 7L, null)));
    }

    @Test
    void vinculoExigeOPerfilCorrespondente() {
        salvarAtribuindoId(5L);

        assertThrows(NegocioException.class, () -> service.criar(new UsuarioCriarRequest(
                "Ana", "ana@petshop.com", null, "senha1234", Set.of(Perfil.CLIENTE), true, 7L, null)));
    }

    @Test
    void naoPermiteDesativarAPropriaConta() {
        Usuario eu = admin(1L, "eu@petshop.com");
        when(usuarios.findById(1L)).thenReturn(Optional.of(eu));

        assertThrows(NegocioException.class, () -> service.atualizar(1L, new UsuarioAtualizarRequest(
                "Eu", "eu@petshop.com", null, Set.of(Perfil.ADMINISTRADOR), false, null, null), "eu@petshop.com"));
    }

    @Test
    void naoPermiteRemoverOPerfilAdministradorDaPropriaConta() {
        Usuario eu = admin(1L, "eu@petshop.com");
        when(usuarios.findById(1L)).thenReturn(Optional.of(eu));

        assertThrows(NegocioException.class, () -> service.atualizar(1L, new UsuarioAtualizarRequest(
                "Eu", "eu@petshop.com", null, Set.of(Perfil.PROFISSIONAL), true, null, null), "eu@petshop.com"));
    }

    @Test
    void preservaOUltimoAdministradorAtivo() {
        Usuario outra = admin(2L, "outra@petshop.com");
        when(usuarios.findById(2L)).thenReturn(Optional.of(outra));
        when(usuarios.contarAtivosComPerfil(Perfil.ADMINISTRADOR, 1L)).thenReturn(1L);

        assertThrows(NegocioException.class, () -> service.atualizar(2L, new UsuarioAtualizarRequest(
                "Outra", "outra@petshop.com", null, Set.of(Perfil.ADMINISTRADOR), false, null, null), "eu@petshop.com"));
    }

    @Test
    void desativaAdministradorQuandoHaOutroAtivo() {
        Usuario outra = admin(2L, "outra@petshop.com");
        when(usuarios.findById(2L)).thenReturn(Optional.of(outra));
        when(usuarios.contarAtivosComPerfil(Perfil.ADMINISTRADOR, 1L)).thenReturn(2L);
        salvarAtribuindoId(2L);

        UsuarioResponse resp = service.atualizar(2L, new UsuarioAtualizarRequest(
                "Outra", "outra@petshop.com", null, Set.of(Perfil.ADMINISTRADOR), false, null, null), "eu@petshop.com");

        assertFalse(resp.ativo());
    }

    @Test
    void trocaDeSenhaExigeASenhaAtual() {
        Usuario u = admin(1L, "eu@petshop.com");
        when(usuarios.findByEmailIgnoreCase("eu@petshop.com")).thenReturn(Optional.of(u));
        when(encoder.matches("errada", "hash")).thenReturn(false);

        assertThrows(NegocioException.class,
                () -> service.alterarSenha("eu@petshop.com", new AlterarSenhaRequest("errada", "novasenha1")));
        verify(usuarios, never()).save(any());
    }

    @Test
    void trocaDeSenhaGravaOHashDaNova() {
        Usuario u = admin(1L, "eu@petshop.com");
        when(usuarios.findByEmailIgnoreCase("eu@petshop.com")).thenReturn(Optional.of(u));
        when(encoder.matches("atual123", "hash")).thenReturn(true);
        when(encoder.encode("novasenha1")).thenReturn("NOVO_HASH");

        service.alterarSenha("eu@petshop.com", new AlterarSenhaRequest("atual123", "novasenha1"));

        assertEquals("NOVO_HASH", u.getSenha());
    }

    @Test
    void usuarioDeOutraLojaNaoEhEncontrado() {
        Usuario deOutraLoja = Usuario.builder().lojaId(2L).id(9L).nome("Intruso").email("x@y.com").senha("hash")
                .perfis(new HashSet<>(Set.of(Perfil.ADMINISTRADOR))).ativo(true).build();
        when(usuarios.findById(9L)).thenReturn(Optional.of(deOutraLoja));

        assertThrows(com.pelomacio.exception.RecursoNaoEncontradoException.class, () -> service.buscar(9L));
    }
}
