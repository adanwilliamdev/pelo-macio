package com.pelomacio.scheduler;

import com.pelomacio.integration.whatsapp.WhatsAppService;
import com.pelomacio.model.Agendamento;
import com.pelomacio.model.Loja;
import com.pelomacio.repository.AgendamentoRepository;
import com.pelomacio.repository.LojaRepository;
import com.pelomacio.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Lembrete diário por WhatsApp. Roda uma vez para cada loja, sempre com a loja definida no contexto. */
@Component
public class LembreteScheduler {

    private static final Logger log = LoggerFactory.getLogger(LembreteScheduler.class);

    private final AgendamentoRepository agendamentos;
    private final LojaRepository lojas;
    private final WhatsAppService whatsApp;
    private final TransactionTemplate tx;

    public LembreteScheduler(AgendamentoRepository agendamentos, LojaRepository lojas, WhatsAppService whatsApp,
                             TransactionTemplate tx) {
        this.agendamentos = agendamentos;
        this.lojas = lojas;
        this.whatsApp = whatsApp;
        this.tx = tx;
    }

    @Scheduled(cron = "${pelomacio.scheduler.lembrete-cron}")
    public void enviarLembretes() {
        for (Loja loja : lojas.findAll()) {
            try {
                // a transação precisa começar DEPOIS de a loja ser definida: é nesse momento que o Hibernate fixa a loja da sessão
                TenantContext.executarComo(loja.getId(), () -> tx.executeWithoutResult(s -> enviarDaLoja(loja)));
            } catch (Exception e) {
                log.error("Lembretes da loja {} falharam: {}", loja.getSlug(), e.getMessage());
            }
        }
    }

    private void enviarDaLoja(Loja loja) {
        LocalDate amanha = LocalDate.now().plusDays(1);
        List<Agendamento> lista = agendamentos.findByDataHoraInicioBetweenAndStatusInAndLembreteEnviadoFalse(
                amanha.atStartOfDay(), amanha.plusDays(1).atStartOfDay(),
                List.of(Agendamento.Status.AGENDADO, Agendamento.Status.CONFIRMADO));
        log.info("Lembretes [{}]: {} agendamento(s) para {}", loja.getSlug(), lista.size(), amanha);

        DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
        for (Agendamento a : lista) {
            String msg = "Olá, %s! Lembrete: %s tem %s amanhã às %s na %s, com %s. Até lá!".formatted(
                    a.getCliente().getNome(), a.getPet().getNome(), a.getServico().getNome(),
                    a.getDataHoraInicio().format(hora), loja.getNome(), a.getProfissional().getNome());
            if (whatsApp.enviarMensagem(a.getCliente().getTelefone(), msg)) {
                a.setLembreteEnviado(true);
            }
        }
    }
}
