package com.pelomacio.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Aplica {@link LimitadorDeRequisicoes} ao login e às rotas públicas que escrevem dados. */
public class LimiteDeRequisicoesFilter extends OncePerRequestFilter {

    private final LimitadorDeRequisicoes login = new LimitadorDeRequisicoes(10, 60_000);
    private final LimitadorDeRequisicoes publico = new LimitadorDeRequisicoes(20, 60_000);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LimitadorDeRequisicoes limitador = escolher(request);
        if (limitador != null && !limitador.permitir(origem(request) + "|" + request.getRequestURI())) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream().write(
                    "{\"status\":429,\"message\":\"Muitas tentativas. Aguarde um minuto e tente de novo.\"}"
                            .getBytes(StandardCharsets.UTF_8));
            return;
        }
        chain.doFilter(request, response);
    }

    private LimitadorDeRequisicoes escolher(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return null;
        String uri = request.getRequestURI();
        if (uri.equals("/api/auth/login")) return login;
        if (uri.startsWith("/api/publico/")) return publico;
        return null;
    }

    /** O nginx do projeto sobrescreve X-Real-IP com o IP real; sem proxy, vale o endereço da conexão. */
    private static String origem(HttpServletRequest request) {
        String real = request.getHeader("X-Real-IP");
        return real != null && !real.isBlank() ? real.trim() : request.getRemoteAddr();
    }
}
