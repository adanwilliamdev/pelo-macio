package com.pelomacio.security;

import com.pelomacio.model.Usuario;
import com.pelomacio.repository.UsuarioRepository;
import com.pelomacio.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Instanciado no SecurityConfig (não é @Component para não registrar duas vezes).
 *
 * <p>O token só prova a identidade: usuário e perfis são lidos do banco a cada requisição. Assim, desativar um
 * usuário ou mudar seus perfis pela interface vale imediatamente, sem esperar o token expirar.
 *
 * <p>A loja da requisição também vem do banco (usuarios.loja_id), nunca do token nem de cabeçalho enviado pelo
 * cliente: é o que impede uma loja de enxergar os dados de outra.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarios;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarios) {
        this.jwtService = jwtService;
        this.usuarios = usuarios;
    }

    /** Rotas públicas ignoram credenciais: a loja delas vem sempre do slug da URL, nunca do usuário logado. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/publico/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.validar(header.substring(7));
                Optional<Usuario> usuario = claims.getSubject() == null ? Optional.empty()
                        : usuarios.findByEmailIgnoreCase(claims.getSubject())
                                .filter(u -> Boolean.TRUE.equals(u.getAtivo()));
                if (usuario.isPresent()) {
                    Usuario u = usuario.get();
                    List<GrantedAuthority> authorities = u.getPerfis().stream()
                            .<GrantedAuthority>map(p -> new SimpleGrantedAuthority("ROLE_" + p.name()))
                            .toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(u.getEmail(), null, authorities));
                    TenantContext.definir(u.getLojaId());
                } else {
                    SecurityContextHolder.clearContext(); // usuário removido/inativo -> 401
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext(); // segue sem autenticação -> 401
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.limpar();
        }
    }
}
