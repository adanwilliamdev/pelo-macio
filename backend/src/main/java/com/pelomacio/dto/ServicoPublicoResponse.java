package com.pelomacio.dto;

import com.pelomacio.model.Pet.Porte;
import com.pelomacio.model.Servico;
import java.math.BigDecimal;

/** Preço e duração já resolvidos para cada porte (médio e grande caem no valor base quando não definidos). */
public record ServicoPublicoResponse(Long id, String nome, String categoria,
                                     BigDecimal precoPequeno, int duracaoPequeno,
                                     BigDecimal precoMedio, int duracaoMedio,
                                     BigDecimal precoGrande, int duracaoGrande) {

    public static ServicoPublicoResponse de(Servico s) {
        return new ServicoPublicoResponse(s.getId(), s.getNome(), s.getCategoria(),
                s.precoPara(Porte.PEQUENO), s.duracaoPara(Porte.PEQUENO),
                s.precoPara(Porte.MEDIO), s.duracaoPara(Porte.MEDIO),
                s.precoPara(Porte.GRANDE), s.duracaoPara(Porte.GRANDE));
    }
}
