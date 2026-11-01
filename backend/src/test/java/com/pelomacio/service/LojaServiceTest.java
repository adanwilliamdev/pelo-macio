package com.pelomacio.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.pelomacio.repository.LojaRepository;
import org.junit.jupiter.api.Test;

class LojaServiceTest {

    private final LojaRepository lojas = mock(LojaRepository.class);
    private final LojaService service = new LojaService(lojas, null, null, null, null, null, null);

    @Test
    void geraSlugSemAcentosEEspacos() {
        assertEquals("patas-felizes-banho-tosa", service.slugLivre("  Patas Felizes — Banho & Tosa! "));
        assertEquals("cao-e-gato", service.slugLivre("Cão é Gato"));
    }

    @Test
    void acrescentaSufixoQuandoOSlugJaExiste() {
        when(lojas.existsBySlug("pet-shop")).thenReturn(true);
        when(lojas.existsBySlug("pet-shop-2")).thenReturn(true);

        assertEquals("pet-shop-3", service.slugLivre("Pet Shop"));
    }

    @Test
    void nomeSemLetrasViraLoja() {
        assertEquals("loja", service.slugLivre("!!!"));
    }
}
