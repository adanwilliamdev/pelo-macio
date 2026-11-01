package com.pelomacio.dto;

import com.pelomacio.model.Cliente;
import com.pelomacio.model.Profissional;
import com.pelomacio.model.Usuario;
import java.util.List;

/** Visão de um usuário para a interface. Nunca inclui a senha. */
public record UsuarioResponse(
        Long id, String nome, String email, String telefone, boolean ativo, List<String> perfis,
        Long profissionalId, String profissionalNome, Long clienteId, String clienteNome) {

    public static UsuarioResponse de(Usuario u, Profissional profissional, Cliente cliente) {
        List<String> perfis = u.getPerfis() == null ? List.of()
                : u.getPerfis().stream().map(Enum::name).sorted().toList();
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(),
                Boolean.TRUE.equals(u.getAtivo()), perfis,
                profissional != null ? profissional.getId() : null,
                profissional != null ? profissional.getNome() : null,
                cliente != null ? cliente.getId() : null,
                cliente != null ? cliente.getNome() : null);
    }
}
