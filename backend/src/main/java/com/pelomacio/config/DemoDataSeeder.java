package com.pelomacio.config;

import com.pelomacio.model.*;
import com.pelomacio.model.Agendamento.Status;
import com.pelomacio.model.Pet.Especie;
import com.pelomacio.model.Pet.Porte;
import com.pelomacio.repository.*;
import com.pelomacio.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Dados de demonstração (liga com PELOMACIO_DEMO=true). Cria duas lojas independentes para mostrar o isolamento:
 * cada uma só enxerga os próprios tutores, pets, agenda e caixa.
 */
@Configuration
@ConditionalOnProperty(name = "pelomacio.demo.enabled", havingValue = "true")
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    @Bean
    CommandLineRunner popularDemonstracao(LojaRepository lojas, UsuarioRepository usuarios, PasswordEncoder encoder,
                                          TransactionTemplate tx, DemoLoja demo,
                                          @Value("${pelomacio.demo.senha}") String senha,
                                          @Value("${pelomacio.timezone}") String timezone) {
        return args -> {
            LocalDateTime agora = LocalDateTime.now(Clock.system(ZoneId.of(timezone)));
            criar(lojas, usuarios, encoder, tx, demo, senha, agora, "Pelo Macio Demo", "demo", "demo@pelomacio.app", 0);
            criar(lojas, usuarios, encoder, tx, demo, senha, agora, "Patas Felizes", "patas-felizes", "patas@pelomacio.app", 1);
        };
    }

    private void criar(LojaRepository lojas, UsuarioRepository usuarios, PasswordEncoder encoder, TransactionTemplate tx,
                       DemoLoja demo, String senha, LocalDateTime agora, String nome, String slug, String email, int variante) {
        if (lojas.existsBySlug(slug)) return;
        Loja loja = lojas.save(Loja.builder().nome(nome).slug(slug).telefone("(11) 4002-8922").build());
        usuarios.save(Usuario.builder().lojaId(loja.getId()).nome("Administrador " + nome).email(email)
                .senha(encoder.encode(senha)).perfis(Set.of(Usuario.Perfil.ADMINISTRADOR)).build());
        // transação aberta DEPOIS de definir a loja: é aí que o Hibernate fixa a loja da sessão
        TenantContext.executarComo(loja.getId(), () -> tx.executeWithoutResult(s -> demo.popular(agora, variante)));
        log.info("Loja de demonstração '{}' criada (login: {})", slug, email);
    }
}
