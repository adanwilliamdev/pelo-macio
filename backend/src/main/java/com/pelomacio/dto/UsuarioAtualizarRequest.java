package com.pelomacio.dto;

import com.pelomacio.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** A senha não é alterada aqui: use a redefinição (administrador) ou a troca da própria senha. */
public record UsuarioAtualizarRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(max = 120, message = "Nome muito longo") String nome,
        @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido")
        @Size(max = 160, message = "E-mail muito longo") String email,
        @Size(max = 30, message = "Telefone muito longo") String telefone,
        @NotEmpty(message = "Selecione ao menos um perfil") Set<Usuario.Perfil> perfis,
        Boolean ativo,
        Long profissionalId,
        Long clienteId) {}
