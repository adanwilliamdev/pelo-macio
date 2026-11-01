package com.pelomacio.service;

import com.pelomacio.dto.AgendamentoRequest;
import com.pelomacio.exception.NegocioException;
import com.pelomacio.exception.RecursoNaoEncontradoException;
import com.pelomacio.integration.google.GoogleCalendarService;
import com.pelomacio.model.*;
import com.pelomacio.model.Agendamento.Status;
import com.pelomacio.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AgendamentoService {

    private static final Set<Status> INATIVOS = Set.of(Status.CANCELADO, Status.FALTOU);

    private final AgendamentoRepository agendamentos;
    private final PetRepository pets;
    private final ProfissionalRepository profissionais;
    private final ServicoRepository servicos;
    private final ProdutoRepository produtos;
    private final LancamentoRepository lancamentos;
    private final GoogleCalendarService calendar;

    public AgendamentoService(AgendamentoRepository agendamentos, PetRepository pets,
                              ProfissionalRepository profissionais, ServicoRepository servicos,
                              ProdutoRepository produtos, LancamentoRepository lancamentos,
                              GoogleCalendarService calendar) {
        this.agendamentos = agendamentos;
        this.pets = pets;
        this.profissionais = profissionais;
        this.servicos = servicos;
        this.produtos = produtos;
        this.lancamentos = lancamentos;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public List<Agendamento> listar(LocalDate de, LocalDate ate, Long profissionalId) {
        List<Agendamento> lista = agendamentos.findByDataHoraInicioBetweenOrderByDataHoraInicio(
                de.atStartOfDay(), ate.plusDays(1).atStartOfDay());
        if (profissionalId == null) return lista;
        return lista.stream().filter(a -> a.getProfissional().getId().equals(profissionalId)).toList();
    }

    @Transactional
    public Agendamento criar(AgendamentoRequest req) {
        return criar(req, Agendamento.Origem.INTERNA);
    }

    @Transactional
    public Agendamento criar(AgendamentoRequest req, Agendamento.Origem origem) {
        Agendamento a = new Agendamento();
        a.setOrigem(origem);
        preencher(a, req);
        Agendamento salvo = agendamentos.save(a);
        calendar.sincronizar(salvo);
        return salvo;
    }

    @Transactional
    public Agendamento atualizar(Long id, AgendamentoRequest req) {
        Agendamento a = buscar(id);
        if (a.getStatus() == Status.CONCLUIDO) {
            throw new NegocioException("Atendimento concluído não pode ser alterado.");
        }
        Profissional anterior = a.getProfissional();
        String eventoAnterior = a.getGoogleEventId();
        preencher(a, req);
        Agendamento salvo = agendamentos.save(a);
        if (!anterior.getId().equals(salvo.getProfissional().getId())) {
            // o evento vivia na agenda da profissional anterior
            calendar.removerEvento(anterior, eventoAnterior);
            salvo.setGoogleEventId(null);
        }
        calendar.sincronizar(salvo);
        return salvo;
    }

    @Transactional
    public Agendamento mudarStatus(Long id, Status novo) {
        Agendamento a = buscar(id);
        if (a.getStatus() == Status.CONCLUIDO && novo != Status.CONCLUIDO) {
            throw new NegocioException("Atendimento concluído não pode mudar de status.");
        }
        if (a.getStatus() != Status.CONCLUIDO && novo == Status.CONCLUIDO) {
            concluir(a);
        }
        Status anterior = a.getStatus();
        a.setStatus(novo);
        Agendamento salvo = agendamentos.save(a);
        // o Google só muda quando o agendamento entra/sai de cancelado/faltou
        if (INATIVOS.contains(anterior) != INATIVOS.contains(novo)) {
            calendar.sincronizar(salvo);
        }
        return salvo;
    }

    @Transactional
    public void remover(Long id) {
        Agendamento a = buscar(id);
        if (a.getStatus() == Status.CONCLUIDO) {
            throw new NegocioException("Atendimento concluído não pode ser excluído.");
        }
        calendar.removerEvento(a.getProfissional(), a.getGoogleEventId());
        agendamentos.delete(a);
    }

    private Agendamento buscar(Long id) {
        return agendamentos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
    }

    private void preencher(Agendamento a, AgendamentoRequest req) {
        Pet pet = pets.findById(req.petId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pet", req.petId()));
        Profissional prof = profissionais.findById(req.profissionalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional", req.profissionalId()));
        Servico servico = servicos.findById(req.servicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço", req.servicoId()));

        LocalDateTime inicio = req.inicio();
        // a duração depende do porte do pet (um banho em cão grande leva mais que em um pequeno)
        LocalDateTime fim = inicio.plusMinutes(servico.duracaoPara(pet.getPorte()));
        boolean encaixe = Boolean.TRUE.equals(req.encaixe());

        if (!encaixe) {
            long conflitos = agendamentos.contarConflitos(prof.getId(), inicio, fim, INATIVOS,
                    a.getId() == null ? -1L : a.getId());
            if (conflitos > 0) {
                throw new NegocioException(
                        "O profissional já tem atendimento nesse horário. Marque como encaixe para permitir simultâneo.",
                        HttpStatus.CONFLICT);
            }
            if (calendar.temCompromissoExterno(prof, inicio, fim, a.getGoogleEventId())) {
                throw new NegocioException(
                        "O profissional tem um compromisso no Google Calendar nesse horário. Marque como encaixe para ignorar.",
                        HttpStatus.CONFLICT);
            }
        }
        a.setCliente(pet.getCliente());
        a.setPet(pet);
        a.setProfissional(prof);
        a.setServico(servico);
        a.setDataHoraInicio(inicio);
        a.setDataHoraFim(fim);
        a.setEncaixe(encaixe);
        a.setLembreteEnviado(false);
    }

    /** Baixa de estoque dos insumos + receita do serviço + comissão da profissional. */
    private void concluir(Agendamento a) {
        Servico servico = a.getServico();
        for (ServicoInsumo insumo : servico.getInsumos()) {
            Produto p = produtos.findById(insumo.getProduto().getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", insumo.getProduto().getId()));
            BigDecimal restante = p.getQuantidadeEstoque().subtract(insumo.getQuantidade());
            if (restante.signum() < 0) {
                throw new NegocioException("Estoque insuficiente de " + p.getNome() + ".");
            }
            p.setQuantidadeEstoque(restante);
            produtos.save(p);
        }
        LocalDate hoje = LocalDate.now();
        BigDecimal preco = servico.precoPara(a.getPet().getPorte());
        String descricao = servico.getNome() + " - " + a.getPet().getNome() + " (" + a.getCliente().getNome() + ")";
        lancamentos.save(Lancamento.builder()
                .tipo(Lancamento.Tipo.RECEITA).categoria("SERVICO").descricao(descricao)
                .valor(preco).data(hoje).agendamentoId(a.getId())
                .profissional(a.getProfissional()).build());

        BigDecimal comissao = preco
                .multiply(a.getProfissional().getComissaoPadrao())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (comissao.signum() > 0) {
            lancamentos.save(Lancamento.builder()
                    .tipo(Lancamento.Tipo.DESPESA).categoria("COMISSAO")
                    .descricao("Comissão: " + descricao).valor(comissao).data(hoje)
                    .agendamentoId(a.getId()).profissional(a.getProfissional()).build());
        }
    }
}
