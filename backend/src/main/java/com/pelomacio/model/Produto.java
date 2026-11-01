package com.pelomacio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Produto implements Identificavel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @TenantId
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    private String tipo;

    @NotNull @DecimalMin("0")
    @Builder.Default
    private BigDecimal quantidadeEstoque = BigDecimal.ZERO;
    private String unidade;

    @NotNull @DecimalMin("0")
    @Builder.Default
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @NotNull @DecimalMin("0")
    @Builder.Default
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;
}
