package com.pelomacio.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.pelomacio.dto.AgendamentoRequest;
import com.pelomacio.exception.NegocioException;
import com.pelomacio.integration.google.GoogleCalendarService;
import com.pelomacio.model.*;
import com.pelomacio.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

class AgendamentoServiceTest {

    private final AgendamentoRepository agendamentos = mock(AgendamentoRepository.class);
    private final PetRepository pets = mock(PetRepository.class);
    private final ProfissionalRepository profissionais = mock(ProfissionalRepository.class);
    private final ServicoRepository servicos = mock(ServicoRepository.class);
    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final LancamentoRepository lancamentos = mock(LancamentoRepository.class);

    private final GoogleCalendarService calendar = mock(GoogleCalendarService.class);

    private final AgendamentoService service = new AgendamentoService(agendamentos, pets, profissionais,
            servicos, produtos, lancamentos, calendar);

    private AgendamentoRequest prepararCenario(boolean encaixe, long conflitos) {
        Cliente ana = Cliente.builder().id(1L).nome("Ana").build();
        when(pets.findById(1L)).thenReturn(Optional.of(
                Pet.builder().id(1L).cliente(ana).nome("Thor").especie(Pet.Especie.CACHORRO).porte(Pet.Porte.PEQUENO).build()));
        when(profissionais.findById(2L)).thenReturn(Optional.of(
                Profissional.builder().id(2L).nome("Bia").comissaoPadrao(BigDecimal.valueOf(40)).build()));
        when(servicos.findById(3L)).thenReturn(Optional.of(
                Servico.builder().id(3L).nome("Banho e tosa").duracaoMinutos(60).preco(BigDecimal.valueOf(50)).build()));
        when(agendamentos.contarConflitos(anyLong(), any(), any(), any(), anyLong())).thenReturn(conflitos);
        when(agendamentos.save(any())).thenAnswer(i -> i.getArgument(0));
        return new AgendamentoRequest(1L, 2L, 3L, LocalDateTime.of(2026, 11, 2, 10, 0), encaixe);
    }

    @Test
    void bloqueiaConflitoDeHorario() {
        AgendamentoRequest req = prepararCenario(false, 1);
        assertThrows(NegocioException.class, () -> service.criar(req));
    }

    @Test
    void permiteEncaixeSimultaneo() {
        AgendamentoRequest req = prepararCenario(true, 1);
        Agendamento a = service.criar(req);
        assertEquals(LocalDateTime.of(2026, 11, 2, 11, 0), a.getDataHoraFim());
        assertTrue(a.getEncaixe());
    }

    @Test
    void concluirGeraReceitaEComissao() {
        prepararCenario(false, 0);
        Agendamento a = service.criar(new AgendamentoRequest(1L, 2L, 3L, LocalDateTime.of(2026, 11, 2, 10, 0), false));
        a.setId(9L);
        when(agendamentos.findById(9L)).thenReturn(Optional.of(a));

        service.mudarStatus(9L, Agendamento.Status.CONCLUIDO);

        verify(lancamentos, times(2)).save(any(Lancamento.class)); // receita + comissão (R$ 20,00)
        assertThrows(NegocioException.class, () -> service.mudarStatus(9L, Agendamento.Status.CANCELADO));
    }

    @Test
    void bloqueiaQuandoHaCompromissoNoGoogleCalendar() {
        AgendamentoRequest req = prepararCenario(false, 0);
        when(calendar.temCompromissoExterno(any(), any(), any(), any())).thenReturn(true);

        assertThrows(NegocioException.class, () -> service.criar(req));
    }

    @Test
    void encaixeIgnoraOGoogleCalendar() {
        AgendamentoRequest req = prepararCenario(true, 0);
        when(calendar.temCompromissoExterno(any(), any(), any(), any())).thenReturn(true);

        assertDoesNotThrow(() -> service.criar(req));
    }

    @Test
    void criarSincronizaComOGoogleCalendar() {
        AgendamentoRequest req = prepararCenario(false, 0);

        Agendamento a = service.criar(req);

        verify(calendar).sincronizar(a);
    }

    @Test
    void cancelarRemoveDoGoogleMasConfirmarNao() {
        prepararCenario(false, 0);
        Agendamento a = service.criar(new AgendamentoRequest(1L, 2L, 3L, LocalDateTime.of(2026, 11, 2, 10, 0), false));
        a.setId(9L);
        when(agendamentos.findById(9L)).thenReturn(Optional.of(a));
        reset(calendar);

        service.mudarStatus(9L, Agendamento.Status.CONFIRMADO);
        verify(calendar, never()).sincronizar(any());

        service.mudarStatus(9L, Agendamento.Status.CANCELADO);
        verify(calendar).sincronizar(a);
    }

    @Test
    void duracaoEPrecoDependemDoPorteDoPet() {
        prepararCenario(false, 0);
        Cliente ana = Cliente.builder().id(1L).nome("Ana").build();
        when(pets.findById(1L)).thenReturn(Optional.of(
                Pet.builder().id(1L).cliente(ana).nome("Thor").especie(Pet.Especie.CACHORRO).porte(Pet.Porte.GRANDE).build()));
        when(servicos.findById(3L)).thenReturn(Optional.of(Servico.builder().id(3L).nome("Banho e tosa")
                .duracaoMinutos(60).preco(BigDecimal.valueOf(50))
                .duracaoGrande(120).precoGrande(BigDecimal.valueOf(100)).build()));

        Agendamento a = service.criar(new AgendamentoRequest(1L, 2L, 3L, LocalDateTime.of(2026, 11, 2, 10, 0), false));
        assertEquals(LocalDateTime.of(2026, 11, 2, 12, 0), a.getDataHoraFim());

        a.setId(9L);
        when(agendamentos.findById(9L)).thenReturn(Optional.of(a));
        service.mudarStatus(9L, Agendamento.Status.CONCLUIDO);

        ArgumentCaptor<Lancamento> salvos = ArgumentCaptor.forClass(Lancamento.class);
        verify(lancamentos, times(2)).save(salvos.capture());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(salvos.getAllValues().get(0).getValor()));
        assertEquals(0, BigDecimal.valueOf(40).compareTo(salvos.getAllValues().get(1).getValor())); // 40% de 100
    }

    @Test
    void agendamentoOnlineGuardaAOrigem() {
        AgendamentoRequest req = prepararCenario(false, 0);
        assertEquals(Agendamento.Origem.ONLINE, service.criar(req, Agendamento.Origem.ONLINE).getOrigem());
        assertEquals(Agendamento.Origem.INTERNA, service.criar(req).getOrigem());
    }
}
