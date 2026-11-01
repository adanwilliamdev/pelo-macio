package com.pelomacio.dto;

import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.util.List;

public record LojaAtualizarRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 30) String telefone,
        @NotNull LocalTime abertura,
        @NotNull LocalTime fechamento,
        @NotNull @Min(5) @Max(240) Integer intervaloMinutos,
        @NotEmpty List<@Min(1) @Max(7) Integer> diasFuncionamento,
        boolean agendamentoOnline) {}
