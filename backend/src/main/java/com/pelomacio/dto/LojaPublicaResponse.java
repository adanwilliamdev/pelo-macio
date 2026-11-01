package com.pelomacio.dto;

import com.pelomacio.model.Loja;
import java.time.LocalTime;
import java.util.List;

/** O que a página pública mostra da loja. Nada interno (ids de usuário, configurações) sai daqui. */
public record LojaPublicaResponse(String nome, String slug, String telefone, LocalTime abertura,
                                  LocalTime fechamento, List<Integer> diasFuncionamento) {

    public static LojaPublicaResponse de(Loja l) {
        return new LojaPublicaResponse(l.getNome(), l.getSlug(), l.getTelefone(), l.getAbertura(), l.getFechamento(),
                l.diasAbertos().stream().map(d -> d.getValue()).sorted().toList());
    }
}
