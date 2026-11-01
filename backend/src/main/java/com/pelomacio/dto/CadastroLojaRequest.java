package com.pelomacio.dto;

import jakarta.validation.constraints.*;

public record CadastroLojaRequest(
        @NotBlank @Size(max = 120) String nomeLoja,
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres") String senha) {}
