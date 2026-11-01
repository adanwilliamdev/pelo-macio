package com.pelomacio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados que o próprio usuário pode editar em "Minha conta". */
public record PerfilRequest(
        @NotBlank(message = "Nome é obrigatório") @Size(max = 120, message = "Nome muito longo") String nome,
        @Size(max = 30, message = "Telefone muito longo") String telefone) {}
