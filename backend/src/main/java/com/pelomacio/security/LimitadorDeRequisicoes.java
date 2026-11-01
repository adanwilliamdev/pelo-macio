package com.pelomacio.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Limite simples de requisições por chave (janela fixa, em memória).
 *
 * <p>Serve para segurar força bruta no login e abuso das rotas públicas. É por instância da aplicação: com várias
 * réplicas o limite efetivo é multiplicado, e o certo seria um limitador compartilhado (Redis) ou no proxy.
 */
public class LimitadorDeRequisicoes {

    private static final int LIMITE_DE_CHAVES = 10_000;

    private record Janela(long inicio, int contagem) {}

    private final int maximo;
    private final long janelaMs;
    private final LongSupplier relogio;
    private final ConcurrentHashMap<String, Janela> janelas = new ConcurrentHashMap<>();

    public LimitadorDeRequisicoes(int maximo, long janelaMs) {
        this(maximo, janelaMs, System::currentTimeMillis);
    }

    public LimitadorDeRequisicoes(int maximo, long janelaMs, LongSupplier relogio) {
        this.maximo = maximo;
        this.janelaMs = janelaMs;
        this.relogio = relogio;
    }

    /** @return true se a requisição pode seguir; false se a chave estourou o limite na janela atual. */
    public boolean permitir(String chave) {
        long agora = relogio.getAsLong();
        if (janelas.size() > LIMITE_DE_CHAVES) {
            janelas.entrySet().removeIf(e -> agora - e.getValue().inicio() >= janelaMs);
        }
        Janela nova = janelas.compute(chave, (k, atual) -> {
            if (atual == null || agora - atual.inicio() >= janelaMs) return new Janela(agora, 1);
            return new Janela(atual.inicio(), atual.contagem() + 1);
        });
        return nova.contagem() <= maximo;
    }
}
