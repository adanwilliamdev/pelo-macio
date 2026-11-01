package com.pelomacio.dto;

import java.math.BigDecimal;

public record ResumoFinanceiro(BigDecimal receitas, BigDecimal despesas, BigDecimal saldo) {}
