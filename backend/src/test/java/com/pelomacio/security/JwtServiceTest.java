package com.pelomacio.security;

import static org.junit.jupiter.api.Assertions.*;

import com.pelomacio.model.Usuario;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import java.util.Set;

class JwtServiceTest {

    private static final String SECRET = "uma-chave-de-teste-com-mais-de-32-bytes-ok!";

    @Test
    void geraEValidaToken() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        Usuario u = Usuario.builder().email("a@b.com").perfis(Set.of(Usuario.Perfil.ADMINISTRADOR)).build();

        var claims = jwt.validar(jwt.gerar(u));

        assertEquals("a@b.com", claims.getSubject());
        assertTrue(claims.get("perfis", java.util.List.class).contains("ADMINISTRADOR"));
    }

    @Test
    void rejeitaTokenExpirado() {
        JwtService jwt = new JwtService(SECRET, -1000);
        Usuario u = Usuario.builder().email("a@b.com").perfis(Set.of(Usuario.Perfil.PROFISSIONAL)).build();

        assertThrows(JwtException.class, () -> jwt.validar(jwt.gerar(u)));
    }

    @Test
    void estadoOAuthCarregaAProfissionalEALoja() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        JwtService.EstadoOAuth estado = jwt.validarEstadoOAuth(jwt.gerarEstadoOAuth(42L, 7L));
        assertEquals(42L, estado.profissionalId());
        assertEquals(7L, estado.lojaId());
    }

    @Test
    void tokenDeLoginNaoServeComoEstadoOAuth() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        Usuario u = Usuario.builder().email("a@b.com").perfis(Set.of(Usuario.Perfil.ADMINISTRADOR)).build();

        assertThrows(JwtException.class, () -> jwt.validarEstadoOAuth(jwt.gerar(u)));
    }

    @Test
    void estadoOAuthDeOutraChaveEhRejeitado() {
        String estado = new JwtService(SECRET, 60_000).gerarEstadoOAuth(1L, 1L);
        JwtService outro = new JwtService("outra-chave-de-teste-com-mais-de-32-bytes-ok!", 60_000);

        assertThrows(JwtException.class, () -> outro.validarEstadoOAuth(estado));
    }
}
