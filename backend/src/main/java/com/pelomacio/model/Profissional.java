package com.pelomacio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "profissionais")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Profissional implements Identificavel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @TenantId
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    private String especialidade;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @NotNull @DecimalMin("0") @DecimalMax("100")
    @Builder.Default
    private BigDecimal comissaoPadrao = BigDecimal.ZERO;
    private String googleCalendarId;

    @Builder.Default
    private Boolean ativo = true;
}
