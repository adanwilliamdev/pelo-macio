package com.pelomacio.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** O tutor é o dono do pet: não precisa vir na requisição. */
public record AgendamentoRequest(
        @NotNull Long petId,
        @NotNull Long profissionalId,
        @NotNull Long servicoId,
        @NotNull LocalDateTime inicio,
        Boolean encaixe) {}
