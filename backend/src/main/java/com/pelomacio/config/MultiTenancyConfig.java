package com.pelomacio.config;

import com.pelomacio.tenant.LojaAtualResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiTenancyConfig {

    /** Liga o filtro automático por loja_id (campos marcados com @TenantId) ao resolver acima. */
    @Bean
    HibernatePropertiesCustomizer tenantResolverCustomizer(LojaAtualResolver resolver) {
        return props -> props.put("hibernate.tenant_identifier_resolver", resolver);
    }
}
