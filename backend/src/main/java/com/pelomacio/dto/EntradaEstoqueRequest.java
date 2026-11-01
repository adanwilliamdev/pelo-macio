package com.pelomacio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EntradaEstoqueRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantidade,
        @DecimalMin("0") BigDecimal custoTotal) {}
