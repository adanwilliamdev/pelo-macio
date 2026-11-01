package com.pelomacio.tenant;

import com.pelomacio.exception.NegocioException;
import org.springframework.http.HttpStatus;
import java.util.function.Supplier;

/**
 * Loja da requisição em andamento, guardada na thread.
 *
 * <p>Quem define: o filtro JWT (usuário autenticado), o filtro da página pública (slug da URL), o callback do
 * Google (loja assinada no state) e os jobs agendados (um por loja). O Hibernate lê este valor ao abrir cada
 * sessão (ver {@link LojaAtualResolver}) e passa a filtrar tudo por {@code loja_id}.
 */
public final class TenantContext {

    /** Valor usado quando não há loja: não existe linha com este id, então nenhuma consulta devolve dados. */
    public static final Long SEM_LOJA = -1L;

    private static final ThreadLocal<Long> ATUAL = new ThreadLocal<>();

    private TenantContext() {}

    public static void definir(Long lojaId) {
        ATUAL.set(lojaId);
    }

    /** @return a loja atual, ou {@code null} se nenhuma foi definida. */
    public static Long atual() {
        return ATUAL.get();
    }

    /** @return a loja atual; falha se não houver (nunca deveria acontecer em rotas autenticadas). */
    public static Long exigir() {
        Long id = ATUAL.get();
        if (id == null) {
            throw new NegocioException("Loja não identificada na requisição.", HttpStatus.UNAUTHORIZED);
        }
        return id;
    }

    public static void limpar() {
        ATUAL.remove();
    }

    /** Executa a ação como se fosse da loja informada e restaura a loja anterior ao terminar. */
    public static <T> T executarComo(Long lojaId, Supplier<T> acao) {
        Long anterior = ATUAL.get();
        ATUAL.set(lojaId);
        try {
            return acao.get();
        } finally {
            if (anterior == null) ATUAL.remove(); else ATUAL.set(anterior);
        }
    }

    public static void executarComo(Long lojaId, Runnable acao) {
        executarComo(lojaId, () -> {
            acao.run();
            return null;
        });
    }
}
