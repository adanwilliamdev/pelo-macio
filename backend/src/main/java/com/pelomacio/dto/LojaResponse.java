package com.pelomacio.dto;

import com.pelomacio.model.Loja;
import java.time.LocalTime;
import java.util.List;

public record LojaResponse(Long id, String nome, String slug, String telefone, LocalTime abertura, LocalTime fechamento,
                           Integer intervaloMinutos, List<Integer> diasFuncionamento, boolean agendamentoOnline) {

    public static LojaResponse de(Loja l) {
        return new LojaResponse(l.getId(), l.getNome(), l.getSlug(), l.getTelefone(), l.getAbertura(), l.getFechamento(),
                l.getIntervaloMinutos(), l.diasAbertos().stream().map(d -> d.getValue()).sorted().toList(),
                Boolean.TRUE.equals(l.getAgendamentoOnline()));
    }
}
