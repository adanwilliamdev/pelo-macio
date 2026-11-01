package com.pelomacio.tenant;

import static org.junit.jupiter.api.Assertions.*;

import com.pelomacio.exception.NegocioException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {

    @AfterEach
    void limpar() {
        TenantContext.limpar();
    }

    @Test
    void exigirFalhaSemLoja() {
        assertThrows(NegocioException.class, TenantContext::exigir);
    }

    @Test
    void executarComoTrocaERestauraALoja() {
        TenantContext.definir(1L);

        Long dentro = TenantContext.executarComo(2L, TenantContext::exigir);

        assertEquals(2L, dentro);
        assertEquals(1L, TenantContext.atual());
    }

    @Test
    void executarComoRestauraMesmoQuandoALojaAnteriorNaoExistia() {
        assertThrows(IllegalStateException.class, () -> TenantContext.executarComo(5L, () -> {
            throw new IllegalStateException("falha");
        }));

        assertNull(TenantContext.atual());
    }

    @Test
    void semLojaOResolverDevolveUmIdQueNaoExiste() {
        assertEquals(TenantContext.SEM_LOJA, new LojaAtualResolver().resolveCurrentTenantIdentifier());
        TenantContext.definir(7L);
        assertEquals(7L, new LojaAtualResolver().resolveCurrentTenantIdentifier());
    }
}
