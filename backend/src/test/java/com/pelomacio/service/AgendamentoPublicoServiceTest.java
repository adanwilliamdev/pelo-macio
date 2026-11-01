package com.pelomacio.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.pelomacio.dto.*;
import com.pelomacio.exception.NegocioException;
import com.pelomacio.model.*;
import com.pelomacio.repository.*;
import com.pelomacio.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

/** Relógio fixo em segunda-feira, 02/11/2026, 10:00. A loja de teste abre de segunda a sábado, das 9h às 18h. */
class AgendamentoPublicoServiceTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TERCA = LocalDate.of(2026, 11, 3);

    private final LojaRepository lojas = mock(LojaRepository.class);
    private final ServicoRepository servicos = mock(ServicoRepository.class);
    private final ProfissionalRepository profissionais = mock(ProfissionalRepository.class);
    private final AgendamentoRepository agendamentos = mock(AgendamentoRepository.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final PetRepository pets = mock(PetRepository.class);
    private final AgendamentoService agendamentoService = mock(AgendamentoService.class);

    private final AgendamentoPublicoService service = new AgendamentoPublicoService(lojas, servicos, profissionais,
            agendamentos, clientes, pets, agendamentoService, RELOGIO);

    private final Profissional camila = Profissional.builder().id(10L).nome("Camila").build();
    private final Profissional diego = Profissional.builder().id(11L).nome("Diego").build();

    @BeforeEach
    void cenario() {
        TenantContext.definir(1L);
        when(lojas.findById(1L)).thenReturn(Optional.of(Loja.builder().id(1L).nome("Loja").slug("loja").build()));
        when(servicos.findById(3L)).thenReturn(Optional.of(Servico.builder().id(3L).nome("Banho")
                .duracaoMinutos(60).preco(BigDecimal.valueOf(50))
                .duracaoGrande(120).precoGrande(BigDecimal.valueOf(90)).build()));
        when(profissionais.findAll(any(Sort.class))).thenReturn(List.of(camila, diego));
        when(profissionais.findById(10L)).thenReturn(Optional.of(camila));
        when(agendamentos.findByDataHoraInicioBetweenOrderByDataHoraInicio(any(), any())).thenReturn(List.of());
    }

    @AfterEach
    void sair() {
        TenantContext.limpar();
    }

    private Agendamento ocupado(Profissional p, int hora, int minuto, int minutos) {
        LocalDateTime inicio = TERCA.atTime(hora, minuto);
        return Agendamento.builder().profissional(p).dataHoraInicio(inicio).dataHoraFim(inicio.plusMinutes(minutos))
                .status(Agendamento.Status.AGENDADO).build();
    }

    @Test
    void oferecaHorariosDentroDoFuncionamentoDaLoja() {
        List<HorarioLivre> horarios = service.horarios(3L, Pet.Porte.PEQUENO, TERCA, null);

        assertEquals(TERCA.atTime(9, 0), horarios.get(0).inicio());
        assertEquals(TERCA.atTime(17, 0), horarios.get(horarios.size() - 1).inicio()); // 17h + 60 min = 18h
        assertEquals(17, horarios.size());
    }

    @Test
    void duracaoMaiorDoPorteGrandeReduzOsHorariosDoFim() {
        List<HorarioLivre> horarios = service.horarios(3L, Pet.Porte.GRANDE, TERCA, null);

        assertEquals(TERCA.atTime(16, 0), horarios.get(horarios.size() - 1).inicio()); // 16h + 120 min = 18h
    }

    @Test
    void horarioOcupadoPassaParaOutroProfissionalEFicaIndisponivelSeTodosOcupados() {
        when(agendamentos.findByDataHoraInicioBetweenOrderByDataHoraInicio(any(), any())).thenReturn(List.of(
                ocupado(camila, 9, 0, 60), ocupado(camila, 14, 0, 60), ocupado(diego, 14, 0, 60)));

        List<HorarioLivre> horarios = service.horarios(3L, Pet.Porte.PEQUENO, TERCA, null);

        HorarioLivre as9 = horarios.stream().filter(h -> h.inicio().equals(TERCA.atTime(9, 0))).findFirst().orElseThrow();
        assertEquals(11L, as9.profissionalId()); // Camila ocupada, sobra o Diego
        assertTrue(horarios.stream().noneMatch(h -> h.inicio().equals(TERCA.atTime(14, 0))));
        assertTrue(horarios.stream().noneMatch(h -> h.inicio().equals(TERCA.atTime(13, 30)))); // 13h30 + 60 min invade as 14h
    }

    @Test
    void agendamentoCanceladoNaoBloqueiaOHorario() {
        Agendamento cancelado = ocupado(camila, 9, 0, 60);
        cancelado.setStatus(Agendamento.Status.CANCELADO);
        when(agendamentos.findByDataHoraInicioBetweenOrderByDataHoraInicio(any(), any())).thenReturn(List.of(cancelado));

        List<HorarioLivre> horarios = service.horarios(3L, Pet.Porte.PEQUENO, TERCA, 10L);

        assertEquals(TERCA.atTime(9, 0), horarios.get(0).inicio());
    }

    @Test
    void naoOfereceDomingoNemDiasPassados() {
        assertTrue(service.horarios(3L, Pet.Porte.PEQUENO, LocalDate.of(2026, 11, 8), null).isEmpty()); // domingo
        assertTrue(service.horarios(3L, Pet.Porte.PEQUENO, LocalDate.of(2026, 11, 1), null).isEmpty()); // ontem
    }

    @Test
    void hojeSoOfereceHorariosComPeloMenosUmaHoraDeAntecedencia() {
        List<HorarioLivre> horarios = service.horarios(3L, Pet.Porte.PEQUENO, LocalDate.of(2026, 11, 2), null);

        assertEquals(LocalDate.of(2026, 11, 2).atTime(11, 0), horarios.get(0).inicio()); // agora são 10h
    }

    private ReservaPublicaRequest reserva(LocalDateTime inicio, String telefone) {
        return new ReservaPublicaRequest("Ana", telefone, null, "Thor", Pet.Especie.CACHORRO, "Golden",
                Pet.Porte.PEQUENO, 3L, null, inicio);
    }

    @Test
    void reservaCadastraTutorEPetEMarcaComoOnline() {
        when(clientes.findFirstByTelefone("11987654321")).thenReturn(Optional.empty());
        when(clientes.save(any(Cliente.class))).thenAnswer(i -> {
            Cliente c = i.getArgument(0);
            c.setId(5L);
            return c;
        });
        when(pets.findFirstByClienteIdAndNomeIgnoreCase(eq(5L), any())).thenReturn(Optional.empty());
        when(pets.save(any(Pet.class))).thenAnswer(i -> {
            Pet p = i.getArgument(0);
            p.setId(8L);
            return p;
        });
        when(agendamentoService.criar(any(), eq(Agendamento.Origem.ONLINE))).thenAnswer(i -> {
            AgendamentoRequest r = i.getArgument(0);
            return Agendamento.builder().id(99L).dataHoraInicio(r.inicio()).build();
        });

        ReservaPublicaResponse resp = service.reservar(reserva(TERCA.atTime(10, 0), "(11) 98765-4321"));

        ArgumentCaptor<AgendamentoRequest> req = ArgumentCaptor.forClass(AgendamentoRequest.class);
        verify(agendamentoService).criar(req.capture(), eq(Agendamento.Origem.ONLINE));
        assertEquals(8L, req.getValue().petId());
        assertEquals(10L, req.getValue().profissionalId()); // primeiro livre
        assertEquals(99L, resp.protocolo());
        assertEquals(0, BigDecimal.valueOf(50).compareTo(resp.valor()));
    }

    @Test
    void reservaReaproveitaTutorEPetJaCadastrados() {
        Cliente existente = Cliente.builder().id(5L).nome("Ana Maria").telefone("11987654321").build();
        Pet pet = Pet.builder().id(8L).cliente(existente).nome("Thor").porte(Pet.Porte.PEQUENO).build();
        when(clientes.findFirstByTelefone("11987654321")).thenReturn(Optional.of(existente));
        when(pets.findFirstByClienteIdAndNomeIgnoreCase(5L, "Thor")).thenReturn(Optional.of(pet));
        when(agendamentoService.criar(any(), any())).thenReturn(Agendamento.builder().id(1L).dataHoraInicio(TERCA.atTime(10, 0)).build());

        service.reservar(reserva(TERCA.atTime(10, 0), "11987654321"));

        verify(clientes, never()).save(any());
        verify(pets, never()).save(any());
    }

    @Test
    void rejeitaTelefoneInvalido() {
        assertThrows(NegocioException.class, () -> service.reservar(reserva(TERCA.atTime(10, 0), "123")));
    }

    @Test
    void rejeitaHorariosForaDasRegrasDaLoja() {
        // fora do funcionamento, não alinhado ao intervalo de 30 min, domingo, passado e sem antecedência
        for (LocalDateTime inicio : List.of(TERCA.atTime(8, 0), TERCA.atTime(17, 30), TERCA.atTime(10, 15),
                LocalDate.of(2026, 11, 8).atTime(10, 0), LocalDate.of(2026, 11, 1).atTime(10, 0),
                LocalDate.of(2026, 11, 2).atTime(10, 30))) {
            assertThrows(NegocioException.class, () -> service.reservar(reserva(inicio, "11987654321")),
                    "deveria rejeitar " + inicio);
        }
        verifyNoInteractions(agendamentoService);
    }

    @Test
    void naoReservaQuandoNenhumProfissionalEstaLivre() {
        when(agendamentos.findByDataHoraInicioBetweenOrderByDataHoraInicio(any(), any())).thenReturn(List.of(
                ocupado(camila, 10, 0, 60), ocupado(diego, 10, 0, 60)));

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.reservar(reserva(TERCA.atTime(10, 0), "11987654321")));
        assertEquals(409, ex.getStatus().value());
    }
}
