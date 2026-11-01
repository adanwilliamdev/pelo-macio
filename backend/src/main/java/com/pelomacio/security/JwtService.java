package com.pelomacio.security;

import com.pelomacio.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String USO_OAUTH_GOOGLE = "google-oauth";
    private static final long VALIDADE_ESTADO_OAUTH_MS = 10 * 60 * 1000L;

    private final SecretKey key;
    private final long expiracaoMs;

    public JwtService(@Value("${pelomacio.jwt.secret}") String secret,
                      @Value("${pelomacio.jwt.expiration}") long expiracaoMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    public String gerar(Usuario usuario) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("lojaId", usuario.getLojaId())
                .claim("perfis", usuario.getPerfis().stream().map(Enum::name).toList())
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + expiracaoMs))
                .signWith(key)
                .compact();
    }

    /** Lança JwtException se o token for inválido ou estiver expirado. */
    public Claims validar(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    /**
     * Parâmetro {@code state} do fluxo OAuth do Google: assinado e de curta duração, carrega a profissional
     * que está se conectando e a loja dela (o callback chega sem Authorization, então não há outra forma de
     * saber de qual loja é). Protege o callback contra CSRF/forjamento.
     */
    public String gerarEstadoOAuth(Long profissionalId, Long lojaId) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(USO_OAUTH_GOOGLE)
                .claim("uso", USO_OAUTH_GOOGLE)
                .claim("profissionalId", profissionalId)
                .claim("lojaId", lojaId)
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + VALIDADE_ESTADO_OAUTH_MS))
                .signWith(key)
                .compact();
    }

    /** Conteúdo do {@code state} do OAuth. */
    public record EstadoOAuth(Long profissionalId, Long lojaId) {}

    /** @return profissional e loja; lança JwtException se não for um state válido (ex.: token de login). */
    public EstadoOAuth validarEstadoOAuth(String state) {
        Claims claims = validar(state);
        Object profissionalId = claims.get("profissionalId");
        Object lojaId = claims.get("lojaId");
        if (!USO_OAUTH_GOOGLE.equals(claims.get("uso")) || !(profissionalId instanceof Number) || !(lojaId instanceof Number)) {
            throw new JwtException("State OAuth inválido.");
        }
        return new EstadoOAuth(((Number) profissionalId).longValue(), ((Number) lojaId).longValue());
    }
}
