package com.pelomacio.dto;

import java.time.LocalDateTime;

public record HorarioLivre(LocalDateTime inicio, Long profissionalId, String profissionalNome) {}
