package com.pelomacio.integration.google;

import static org.junit.jupiter.api.Assertions.*;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.junit.jupiter.api.Test;
import java.util.Map;

class GoogleCalendarServiceTest {

    private static Event comHorario() {
        Event e = new Event();
        e.setId("evt-1");
        e.setStatus("confirmed");
        EventDateTime inicio = new EventDateTime();
        inicio.setDateTime(new DateTime(System.currentTimeMillis()));
        e.setStart(inicio);
        return e;
    }

    @Test
    void compromissoComHorarioBloqueia() {
        assertTrue(GoogleCalendarService.bloqueia(comHorario(), null));
    }

    @Test
    void eventoCanceladoNaoBloqueia() {
        Event e = comHorario();
        e.setStatus("cancelled");
        assertFalse(GoogleCalendarService.bloqueia(e, null));
    }

    @Test
    void eventoMarcadoComoLivreNaoBloqueia() {
        Event e = comHorario();
        e.setTransparency("transparent");
        assertFalse(GoogleCalendarService.bloqueia(e, null));
    }

    @Test
    void eventoDeDiaInteiroNaoBloqueia() {
        Event e = comHorario();
        e.setStart(new EventDateTime()); // sem dateTime = dia inteiro
        assertFalse(GoogleCalendarService.bloqueia(e, null));
    }

    @Test
    void ignoraOEventoDoProprioAgendamento() {
        assertFalse(GoogleCalendarService.bloqueia(comHorario(), "evt-1"));
        assertTrue(GoogleCalendarService.bloqueia(comHorario(), "outro"));
    }

    @Test
    void eventoCriadoPeloPeloMacioNaoContaComoExterno() {
        Event e = comHorario();
        Event.ExtendedProperties extras = new Event.ExtendedProperties();
        extras.setPrivate(Map.of(GoogleCalendarService.MARCA_PELO_MACIO, "10"));
        e.setExtendedProperties(extras);
        assertFalse(GoogleCalendarService.bloqueia(e, null));
    }
}
