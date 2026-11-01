package com.pelomacio.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/** Diz ao Hibernate qual é a loja da sessão que está sendo aberta. */
@Component
public class LojaAtualResolver implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long loja = TenantContext.atual();
        return loja != null ? loja : TenantContext.SEM_LOJA;
    }

    /** Falso: cada transação abre a própria sessão com a loja do momento (jobs trocam de loja na mesma thread). */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
