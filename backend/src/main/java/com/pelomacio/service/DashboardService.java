package com.pelomacio.service;

import com.pelomacio.dto.DashboardResponse;
import com.pelomacio.model.Agendamento;
import com.pelomacio.model.Lancamento;
import com.pelomacio.repository.AgendamentoRepository;
import com.pelomacio.repository.ClienteRepository;
import com.pelomacio.repository.LancamentoRepository;
import com.pelomacio.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class DashboardService {

    private static final Set<Agendamento.Status> INATIVOS = Set.of(Agendamento.Status.CANCELADO, Agendamento.Status.FALTOU);

    private static final int MESES_NA_SERIE = 6;

    private final AgendamentoRepository agendamentos;
    private final ClienteRepository clientes;
    private final LancamentoRepository lancamentos;
    private final ProdutoRepository produtos;

    public DashboardService(AgendamentoRepository agendamentos, ClienteRepository clientes,
                            LancamentoRepository lancamentos, ProdutoRepository produtos) {
        this.agendamentos = agendamentos;
        this.clientes = clientes;
        this.lancamentos = lancamentos;
        this.produtos = produtos;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumo(boolean incluirFinanceiro) {
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioDia = hoje.atStartOfDay();
        LocalDateTime fimDia = hoje.plusDays(1).atStartOfDay();

        long hojeQtd = agendamentos.countByDataHoraInicioBetweenAndStatusNotIn(inicioDia, fimDia, INATIVOS);
        List<BigDecimal> serie = incluirFinanceiro ? receitaUltimosMeses(hoje, MESES_NA_SERIE) : null;
        BigDecimal receita = serie == null ? null : serie.get(serie.size() - 1);
        var proximos = agendamentos
                .findByDataHoraInicioBetweenOrderByDataHoraInicio(LocalDateTime.now(), fimDia.plusDays(2)).stream()
                .filter(a -> !INATIVOS.contains(a.getStatus()) && a.getStatus() != Agendamento.Status.CONCLUIDO)
                .limit(8).toList();
        return new DashboardResponse(hojeQtd, clientes.countByAtivoTrue(), receita, serie, proximos, produtos.findAbaixoDoMinimo());
    }

    /** Receita de cada um dos últimos {@code meses} meses (do mais antigo ao mês de {@code hoje}). */
    private List<BigDecimal> receitaUltimosMeses(LocalDate hoje, int meses) {
        List<BigDecimal> serie = new ArrayList<>(meses);
        for (int i = meses - 1; i >= 0; i--) {
            LocalDate primeiro = hoje.withDayOfMonth(1).minusMonths(i);
            serie.add(lancamentos.somar(Lancamento.Tipo.RECEITA, primeiro, primeiro.withDayOfMonth(primeiro.lengthOfMonth())));
        }
        return serie;
    }
}
