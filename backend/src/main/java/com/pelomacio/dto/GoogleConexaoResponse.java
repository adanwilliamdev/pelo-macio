package com.pelomacio.dto;

import java.time.LocalDateTime;

public record GoogleConexaoResponse(
        Long profissionalId, String profissionalNome, boolean conectado, boolean reautorizar,
        String email, LocalDateTime conectadoEm, String calendarId) {}
