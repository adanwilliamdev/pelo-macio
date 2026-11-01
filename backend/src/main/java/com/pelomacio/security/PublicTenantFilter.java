package com.pelomacio.security;

import com.pelomacio.model.Loja;
import com.pelomacio.repository.LojaRepository;
import com.pelomacio.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rotas públicas de uma loja: {@code /api/publico/lojas/{slug}/...}. Sem login, a loja vem do slug da URL.
 * Slug desconhecido (ou loja com agendamento online desligado) responde 404 sem tocar em nenhum dado.
 */
public class PublicTenantFilter extends OncePerRequestFilter {

    private static final Pattern ROTA = Pattern.compile("^/api/publico/lojas/([a-z0-9-]{1,60})(?:/.*)?$");

    private final LojaRepository lojas;

    public PublicTenantFilter(LojaRepository lojas) {
        this.lojas = lojas;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/publico/lojas/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Matcher m = ROTA.matcher(request.getRequestURI());
        Optional<Loja> loja = m.matches() ? lojas.findBySlug(m.group(1)) : Optional.empty();
        if (loja.isEmpty() || !Boolean.TRUE.equals(loja.get().getAgendamentoOnline())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream().write(
                    "{\"status\":404,\"message\":\"Loja não encontrada ou sem agendamento online.\"}"
                            .getBytes(StandardCharsets.UTF_8));
            return;
        }
        TenantContext.definir(loja.get().getId());
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.limpar();
        }
    }
}
