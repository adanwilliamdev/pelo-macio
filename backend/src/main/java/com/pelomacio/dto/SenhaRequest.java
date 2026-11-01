package com.pelomacio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Redefinição de senha feita pelo administrador. */
public record SenhaRequest(
        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String novaSenha) {}
