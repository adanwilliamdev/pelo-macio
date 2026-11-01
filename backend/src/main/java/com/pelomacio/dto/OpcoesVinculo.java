package com.pelomacio.dto;

import java.util.List;

/** Profissionais e clientes que podem ser vinculados a um usuário (usuarioId != null = já vinculado). */
public record OpcoesVinculo(List<Opcao> profissionais, List<Opcao> clientes) {
    public record Opcao(Long id, String nome, Long usuarioId) {}
}
