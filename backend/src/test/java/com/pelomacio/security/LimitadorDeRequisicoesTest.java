package com.pelomacio.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;

class LimitadorDeRequisicoesTest {

    @Test
    void bloqueiaDepoisDoLimiteEDesbloqueiaNaProximaJanela() {
        AtomicLong agora = new AtomicLong(1_000);
        LimitadorDeRequisicoes limitador = new LimitadorDeRequisicoes(3, 60_000, agora::get);

        assertTrue(limitador.permitir("ip"));
        assertTrue(limitador.permitir("ip"));
        assertTrue(limitador.permitir("ip"));
        assertFalse(limitador.permitir("ip"));

        agora.addAndGet(60_000);
        assertTrue(limitador.permitir("ip"));
    }

    @Test
    void chavesDiferentesTemContagensIndependentes() {
        LimitadorDeRequisicoes limitador = new LimitadorDeRequisicoes(1, 60_000, () -> 0L);

        assertTrue(limitador.permitir("a"));
        assertFalse(limitador.permitir("a"));
        assertTrue(limitador.permitir("b"));
    }
}
