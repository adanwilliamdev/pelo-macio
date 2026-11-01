package com.pelomacio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Confirmação mostrada ao tutor. Não devolve dados cadastrais (o telefone não deve servir para consultar a base). */
public record ReservaPublicaResponse(Long protocolo, LocalDateTime inicio, String loja, String servico,
                                     String profissional, String pet, BigDecimal valor) {}
