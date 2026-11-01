package com.pelomacio.dto;

import com.pelomacio.model.Agendamento;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull Agendamento.Status status) {}
