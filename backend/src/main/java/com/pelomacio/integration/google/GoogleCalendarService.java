package com.pelomacio.integration.google;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.pelomacio.dto.SincronizacaoResultado;
import com.pelomacio.model.Agendamento;
import com.pelomacio.model.Agendamento.Status;
import com.pelomacio.model.Profissional;
import com.pelomacio.repository.AgendamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Sincroniza os agendamentos com o Google Calendar de cado profissional conectada.
 *
 * <p>Princípio: o Google nunca derruba o fluxo do pet shop. Toda falha de rede/permissão é registrada em log e
 * o agendamento segue normalmente (fica sem {@code googleEventId} e entra em "Sincronizar agora").
 */
@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);

    /** Propriedade privada gravada nos eventos criados pelo Pelo Macio, para não tratá-los como "compromisso externo". */
    static final String MARCA_PELO_MACIO = "peloMacioAgendamentoId";

    private static final String AGENDA_PRINCIPAL = "primary";
    private static final Set<Status> INATIVOS = Set.of(Status.CANCELADO, Status.FALTOU);
    private static final List<Status> ATIVOS = List.of(Status.AGENDADO, Status.CONFIRMADO, Status.CONCLUIDO);

    private final GoogleAuthService auth;
    private final AgendamentoRepository agendamentos;
    private final ZoneId zona;

    public GoogleCalendarService(GoogleAuthService auth, AgendamentoRepository agendamentos,
                                 @Value("${pelomacio.timezone:America/Sao_Paulo}") String zona) {
        this.auth = auth;
        this.agendamentos = agendamentos;
        this.zona = ZoneId.of(zona);
    }

    public boolean conectado(Profissional profissional) {
        return cliente(profissional).isPresent();
    }

    /**
     * Há, na agenda Google do profissional, um compromisso com horário marcado que conflita com o intervalo?
     * Eventos criados pelo Pelo Macio, cancelados, "livres" e de dia inteiro não bloqueiam. Se o Google estiver
     * inacessível, responde false (não impede o agendamento).
     *
     * @param ignorarEventoId evento do próprio agendamento em edição (pode ser null)
     */
    public boolean temCompromissoExterno(Profissional profissional, LocalDateTime inicio, LocalDateTime fim,
                                         String ignorarEventoId) {
        Optional<Calendar> cliente = cliente(profissional);
        if (cliente.isEmpty()) return false;
        try {
            Events resultado = cliente.get().events().list(calendarId(profissional))
                    .setTimeMin(dataHora(inicio))
                    .setTimeMax(dataHora(fim))
                    .setSingleEvents(true)
                    .setMaxResults(50)
                    .execute();
            return resultado.getItems() != null
                    && resultado.getItems().stream().anyMatch(e -> bloqueia(e, ignorarEventoId));
        } catch (Exception e) {
            tratarFalha(profissional, "consultar a agenda", e);
            return false;
        }
    }

    /**
     * Faz o Google refletir o agendamento: cria/atualiza o evento quando ativo e remove quando cancelado/faltou.
     * Atualiza {@code googleEventId} no objeto (o chamador persiste). Nunca lança exceção.
     */
    public void sincronizar(Agendamento a) {
        Profissional profissional = a.getProfissional();
        Optional<Calendar> cliente = cliente(profissional);
        if (cliente.isEmpty()) return;

        String calendarId = calendarId(profissional);
        try {
            if (INATIVOS.contains(a.getStatus())) {
                if (a.getGoogleEventId() != null) {
                    apagar(cliente.get(), calendarId, a.getGoogleEventId());
                    a.setGoogleEventId(null);
                }
                return;
            }

            Event evento = montar(a);
            if (a.getGoogleEventId() != null) {
                try {
                    Event atualizado = cliente.get().events().update(calendarId, a.getGoogleEventId(), evento).execute();
                    a.setGoogleEventId(atualizado.getId());
                    return;
                } catch (GoogleJsonResponseException e) {
                    // evento apagado manualmente no Google: recria abaixo
                    if (e.getStatusCode() != 404 && e.getStatusCode() != 410) throw e;
                }
            }
            a.setGoogleEventId(cliente.get().events().insert(calendarId, evento).execute().getId());
        } catch (Exception e) {
            tratarFalha(profissional, "sincronizar o agendamento " + a.getId(), e);
        }
    }

    /** Apaga um evento da agenda do profissional (ex.: agendamento excluído ou trocado de profissional). */
    public void removerEvento(Profissional profissional, String eventoId) {
        if (eventoId == null) return;
        Optional<Calendar> cliente = cliente(profissional);
        if (cliente.isEmpty()) return;
        try {
            apagar(cliente.get(), calendarId(profissional), eventoId);
        } catch (Exception e) {
            tratarFalha(profissional, "remover o evento", e);
        }
    }

    /**
     * Envia ao Google os agendamentos futuros que ainda não têm evento (feitos antes de conectar, ou cuja
     * sincronização falhou). Processa no máximo {@code limite} por chamada.
     */
    @Transactional
    public SincronizacaoResultado sincronizarPendentes(Profissional profissional, int limite) {
        if (cliente(profissional).isEmpty()) return new SincronizacaoResultado(0, 0, 0);

        List<Agendamento> pendentes = agendamentos.pendentesGoogle(profissional.getId(), ATIVOS, LocalDateTime.now(zona));
        int sincronizados = 0;
        int falhas = 0;
        for (Agendamento a : pendentes.stream().limit(limite).toList()) {
            sincronizar(a);
            if (a.getGoogleEventId() != null) sincronizados++;
            else falhas++;
        }
        return new SincronizacaoResultado(sincronizados, falhas, Math.max(0, pendentes.size() - limite));
    }

    // ───────────── internos ─────────────

    /** Regra de bloqueio de um evento do Google. Visível ao pacote para teste. */
    static boolean bloqueia(Event evento, String ignorarEventoId) {
        if ("cancelled".equals(evento.getStatus())) return false;
        if ("transparent".equals(evento.getTransparency())) return false;          // marcado como "livre"
        if (evento.getStart() == null || evento.getStart().getDateTime() == null) return false; // dia inteiro
        if (ignorarEventoId != null && ignorarEventoId.equals(evento.getId())) return false;
        Event.ExtendedProperties extras = evento.getExtendedProperties();
        return extras == null || extras.getPrivate() == null || !extras.getPrivate().containsKey(MARCA_PELO_MACIO);
    }

    private Optional<Calendar> cliente(Profissional profissional) {
        if (!auth.configurado()) return Optional.empty();
        return auth.conexaoAtiva(profissional).map(conexao -> {
            try {
                return new Calendar.Builder(GoogleAuthService.TRANSPORT, GoogleAuthService.JSON, auth.credencial(conexao))
                        .setApplicationName("Pelo Macio")
                        .build();
            } catch (IllegalStateException e) {
                log.warn("Google Calendar: token ilegível para {} ({}). Reconexão necessária.",
                        profissional.getNome(), e.getMessage());
                auth.marcarReautorizar(conexao);
                return null;
            }
        });
    }

    private static String calendarId(Profissional profissional) {
        String id = profissional.getGoogleCalendarId();
        return id == null || id.isBlank() ? AGENDA_PRINCIPAL : id.trim();
    }

    private Event montar(Agendamento a) {
        StringBuilder descricao = new StringBuilder()
                .append("Serviço: ").append(a.getServico().getNome())
                .append("\nPet: ").append(a.getPet().getNome()).append(" (").append(a.getPet().getPorte()).append(")")
                .append("\nTutor: ").append(a.getCliente().getNome());
        String telefone = a.getCliente().getTelefone();
        if (telefone != null && !telefone.isBlank()) descricao.append("\nTelefone: ").append(telefone);
        if (Boolean.TRUE.equals(a.getEncaixe())) descricao.append("\nEncaixe (atendimento simultâneo)");
        descricao.append("\n\nCriado pelo Pelo Macio");

        Event.ExtendedProperties extras = new Event.ExtendedProperties();
        extras.setPrivate(Map.of(MARCA_PELO_MACIO, String.valueOf(a.getId())));

        Event evento = new Event();
        evento.setSummary(a.getServico().getNome() + " - " + a.getPet().getNome());
        evento.setDescription(descricao.toString());
        evento.setStart(horario(a.getDataHoraInicio()));
        evento.setEnd(horario(a.getDataHoraFim()));
        evento.setExtendedProperties(extras);
        return evento;
    }

    private EventDateTime horario(LocalDateTime momento) {
        EventDateTime h = new EventDateTime();
        h.setDateTime(dataHora(momento));
        h.setTimeZone(zona.getId());
        return h;
    }

    private DateTime dataHora(LocalDateTime momento) {
        return new DateTime(momento.atZone(zona).toInstant().toEpochMilli());
    }

    /** Apagar um evento que já não existe é sucesso. */
    private void apagar(Calendar cliente, String calendarId, String eventoId) throws IOException {
        try {
            cliente.events().delete(calendarId, eventoId).execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() != 404 && e.getStatusCode() != 410) throw e;
        }
    }

    private void tratarFalha(Profissional profissional, String acao, Exception e) {
        if (e instanceof TokenResponseException token
                && token.getDetails() != null && "invalid_grant".equals(token.getDetails().getError())) {
            log.warn("Google Calendar: acesso de {} foi revogado. Reconexão necessária.", profissional.getNome());
            auth.conexaoAtiva(profissional).ifPresent(auth::marcarReautorizar);
        } else {
            log.warn("Google Calendar: não foi possível {} de {} ({})", acao, profissional.getNome(), e.getMessage());
        }
    }
}
