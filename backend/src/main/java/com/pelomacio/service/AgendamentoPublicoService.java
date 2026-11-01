package com.pelomacio.service;

import com.pelomacio.dto.*;
import com.pelomacio.exception.NegocioException;
import com.pelomacio.exception.RecursoNaoEncontradoException;
import com.pelomacio.model.*;
import com.pelomacio.model.Agendamento.Status;
import com.pelomacio.repository.*;
import com.pelomacio.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Agendamento feito pelo próprio tutor, sem login. A loja já vem definida no {@link TenantContext} pelo
 * {@code PublicTenantFilter}, então todas as consultas daqui só enxergam a loja do slug.
 */
@Service
public class AgendamentoPublicoService {

    private static final Set<Status> INATIVOS = Set.of(Status.CANCELADO, Status.FALTOU);
    /** Não aceita marcar para daqui a menos de 1 hora. */
    static final int ANTECEDENCIA_MINUTOS = 60;
    /** Só oferece datas dentro dos próximos 60 dias. */
    static final int JANELA_DIAS = 60;

    private final LojaRepository lojas;
    private final ServicoRepository servicos;
    private final ProfissionalRepository profissionais;
    private final AgendamentoRepository agendamentos;
    private final ClienteRepository clientes;
    private final PetRepository pets;
    private final AgendamentoService agendamentoService;
    private final Clock relogio;

    @Autowired
    public AgendamentoPublicoService(LojaRepository lojas, ServicoRepository servicos,
                                     ProfissionalRepository profissionais, AgendamentoRepository agendamentos,
                                     ClienteRepository clientes, PetRepository pets,
                                     AgendamentoService agendamentoService,
                                     @Value("${pelomacio.timezone}") String timezone) {
        this(lojas, servicos, profissionais, agendamentos, clientes, pets, agendamentoService,
                Clock.system(ZoneId.of(timezone)));
    }

    AgendamentoPublicoService(LojaRepository lojas, ServicoRepository servicos, ProfissionalRepository profissionais,
                              AgendamentoRepository agendamentos, ClienteRepository clientes, PetRepository pets,
                              AgendamentoService agendamentoService, Clock relogio) {
        this.lojas = lojas;
        this.servicos = servicos;
        this.profissionais = profissionais;
        this.agendamentos = agendamentos;
        this.clientes = clientes;
        this.pets = pets;
        this.agendamentoService = agendamentoService;
        this.relogio = relogio;
    }

    // ───────────── consulta ─────────────

    @Transactional(readOnly = true)
    public LojaPublicaResponse loja() {
        return LojaPublicaResponse.de(lojaAtual());
    }

    @Transactional(readOnly = true)
    public List<ServicoPublicoResponse> servicos() {
        return servicos.findAll(Sort.by("nome")).stream()
                .filter(s -> !Boolean.FALSE.equals(s.getAtivo()))
                .map(ServicoPublicoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ProfissionalPublicoResponse> profissionais() {
        return profissionaisAtivos().stream()
                .map(p -> new ProfissionalPublicoResponse(p.getId(), p.getNome(), p.getEspecialidade())).toList();
    }

    /**
     * Horários livres do dia para o serviço e o porte do pet. Sem profissional escolhido, cada horário aparece
     * uma vez, com o primeiro profissional livre naquele momento.
     */
    @Transactional(readOnly = true)
    public List<HorarioLivre> horarios(Long servicoId, Pet.Porte porte, LocalDate data, Long profissionalId) {
        Loja loja = lojaAtual();
        Servico servico = servicoDisponivel(servicoId);
        LocalDateTime agora = LocalDateTime.now(relogio);
        if (!dataAtendivel(loja, data, agora)) return List.of();

        List<Profissional> candidatos = profissionalId != null
                ? List.of(profissionalDisponivel(profissionalId)) : profissionaisAtivos();
        List<Agendamento> doDia = agendamentos.findByDataHoraInicioBetweenOrderByDataHoraInicio(
                data.atStartOfDay(), data.plusDays(1).atStartOfDay()).stream()
                .filter(a -> !INATIVOS.contains(a.getStatus())).toList();

        int duracao = servico.duracaoPara(porte);
        List<HorarioLivre> livres = new ArrayList<>();
        LocalDateTime limite = data.atTime(loja.getFechamento());
        for (LocalDateTime inicio = data.atTime(loja.getAbertura());
             !inicio.plusMinutes(duracao).isAfter(limite);
             inicio = inicio.plusMinutes(loja.getIntervaloMinutos())) {
            if (inicio.isBefore(agora.plusMinutes(ANTECEDENCIA_MINUTOS))) continue;
            Optional<Profissional> livre = primeiroLivre(candidatos, doDia, inicio, inicio.plusMinutes(duracao));
            if (livre.isPresent()) {
                livres.add(new HorarioLivre(inicio, livre.get().getId(), livre.get().getNome()));
            }
        }
        return livres;
    }

    // ───────────── reserva ─────────────

    @Transactional
    public ReservaPublicaResponse reservar(ReservaPublicaRequest req) {
        Loja loja = lojaAtual();
        String telefone = normalizarTelefone(req.telefone());
        Servico servico = servicoDisponivel(req.servicoId());
        LocalDateTime inicio = req.inicio();
        validarHorario(loja, inicio, servico.duracaoPara(req.porte()));

        Profissional profissional = req.profissionalId() != null
                ? profissionalDisponivel(req.profissionalId())
                : escolherProfissionalLivre(inicio, servico.duracaoPara(req.porte()));

        Cliente tutor = clientes.findFirstByTelefone(telefone).orElseGet(() -> clientes.save(Cliente.builder()
                .nome(req.nome().trim()).telefone(telefone)
                .email(req.email() == null || req.email().isBlank() ? null : req.email().trim())
                .observacoes("Cadastrado pelo agendamento online.").build()));
        // Se o tutor já existe, o pet é procurado pelo nome; se não existir, cadastra. Dados do cadastro
        // existente nunca são alterados nem devolvidos por esta rota.
        Pet pet = pets.findFirstByClienteIdAndNomeIgnoreCase(tutor.getId(), req.petNome().trim())
                .orElseGet(() -> pets.save(Pet.builder().cliente(tutor).nome(req.petNome().trim())
                        .especie(req.especie()).raca(vazioParaNulo(req.raca())).porte(req.porte()).build()));

        Agendamento a = agendamentoService.criar(new AgendamentoRequest(
                pet.getId(), profissional.getId(), servico.getId(), inicio, false), Agendamento.Origem.ONLINE);

        return new ReservaPublicaResponse(a.getId(), a.getDataHoraInicio(), loja.getNome(), servico.getNome(),
                profissional.getNome(), pet.getNome(), servico.precoPara(pet.getPorte()));
    }

    // ───────────── regras ─────────────

    private void validarHorario(Loja loja, LocalDateTime inicio, int duracao) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        if (!dataAtendivel(loja, inicio.toLocalDate(), agora)) {
            throw new NegocioException("A loja não atende nessa data.");
        }
        if (inicio.isBefore(agora.plusMinutes(ANTECEDENCIA_MINUTOS))) {
            throw new NegocioException("Escolha um horário com pelo menos 1 hora de antecedência.");
        }
        LocalDateTime abre = inicio.toLocalDate().atTime(loja.getAbertura());
        LocalDateTime fecha = inicio.toLocalDate().atTime(loja.getFechamento());
        long desdeAbertura = Duration.between(abre, inicio).toMinutes();
        if (inicio.isBefore(abre) || inicio.plusMinutes(duracao).isAfter(fecha)
                || desdeAbertura % loja.getIntervaloMinutos() != 0) {
            throw new NegocioException("Horário fora do funcionamento da loja.");
        }
    }

    private boolean dataAtendivel(Loja loja, LocalDate data, LocalDateTime agora) {
        return !data.isBefore(agora.toLocalDate())
                && !data.isAfter(agora.toLocalDate().plusDays(JANELA_DIAS))
                && loja.diasAbertos().contains(data.getDayOfWeek());
    }

    private Profissional escolherProfissionalLivre(LocalDateTime inicio, int duracao) {
        List<Agendamento> doDia = agendamentos.findByDataHoraInicioBetweenOrderByDataHoraInicio(
                inicio.toLocalDate().atStartOfDay(), inicio.toLocalDate().plusDays(1).atStartOfDay()).stream()
                .filter(a -> !INATIVOS.contains(a.getStatus())).toList();
        return primeiroLivre(profissionaisAtivos(), doDia, inicio, inicio.plusMinutes(duracao))
                .orElseThrow(() -> new NegocioException("Esse horário não está mais disponível.", HttpStatus.CONFLICT));
    }

    private static Optional<Profissional> primeiroLivre(List<Profissional> candidatos, List<Agendamento> doDia,
                                                       LocalDateTime inicio, LocalDateTime fim) {
        return candidatos.stream().filter(p -> doDia.stream().noneMatch(a ->
                a.getProfissional().getId().equals(p.getId())
                        && a.getDataHoraInicio().isBefore(fim) && a.getDataHoraFim().isAfter(inicio))).findFirst();
    }

    private List<Profissional> profissionaisAtivos() {
        return profissionais.findAll(Sort.by("nome")).stream()
                .filter(p -> !Boolean.FALSE.equals(p.getAtivo())).toList();
    }

    private Profissional profissionalDisponivel(Long id) {
        return profissionais.findById(id).filter(p -> !Boolean.FALSE.equals(p.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", id));
    }

    private Servico servicoDisponivel(Long id) {
        return servicos.findById(id).filter(s -> !Boolean.FALSE.equals(s.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", id));
    }

    private Loja lojaAtual() {
        Long id = TenantContext.exigir();
        return lojas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Loja", id));
    }

    /** Só dígitos, de 10 a 13 (DDD + número, com ou sem DDI). */
    static String normalizarTelefone(String telefone) {
        String digitos = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 13) {
            throw new NegocioException("Informe um telefone válido com DDD.");
        }
        return digitos;
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
