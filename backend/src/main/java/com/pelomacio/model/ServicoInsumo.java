package com.pelomacio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/** Quantidade de um produto consumida a cada execução do serviço (baixa automática). */
@Entity
@Table(name = "servico_insumos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServicoInsumo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    private Produto produto;

    @NotNull @DecimalMin(value = "0", inclusive = false)
    private BigDecimal quantidade;
}
