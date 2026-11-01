package com.pelomacio.dto;

import com.pelomacio.model.Agendamento;
import com.pelomacio.model.Produto;
import java.math.BigDecimal;
import java.util.List;

/**
 * receitaMes e receitaSerie são nulos para perfis sem acesso ao financeiro.
 * receitaSerie traz a receita dos últimos 6 meses, do mais antigo ao mês atual (último item = receitaMes).
 */
public record DashboardResponse(
        long agendamentosHoje,
        long clientesAtivos,
        BigDecimal receitaMes,
        List<BigDecimal> receitaSerie,
        List<Agendamento> proximos,
        List<Produto> estoqueBaixo) {}
