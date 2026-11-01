package com.pelomacio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lancamentos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lancamento implements Identificavel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @TenantId
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    /** SERVICO, COMISSAO, COMPRA_ESTOQUE, OUTROS */
    @NotBlank
    private String categoria;

    @NotBlank
    private String descricao;

    @NotNull @DecimalMin(value = "0", inclusive = false)
    private BigDecimal valor;

    @NotNull
    private LocalDate data;

    private Long agendamentoId;

    @ManyToOne
    private Profissional profissional;

    public enum Tipo { RECEITA, DESPESA }
}
