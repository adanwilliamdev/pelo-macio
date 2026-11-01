package com.pelomacio.dto;

import com.pelomacio.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record UsuarioCriarRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(max = 120, message = "Nome muito longo") String nome,
        @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido")
        @Size(max = 160, message = "E-mail muito longo") String email,
        @Size(max = 30, message = "Telefone muito longo") String telefone,
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha,
        @NotEmpty(message = "Selecione ao menos um perfil") Set<Usuario.Perfil> perfis,
        Boolean ativo,
        Long profissionalId,
        Long clienteId) {}
