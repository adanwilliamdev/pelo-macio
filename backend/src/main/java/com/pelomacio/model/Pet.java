package com.pelomacio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "pets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pet implements Identificavel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @TenantId
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    @NotNull(message = "Tutor é obrigatório")
    @ManyToOne(optional = false)
    private Cliente cliente;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotNull(message = "Espécie é obrigatória")
    @Enumerated(EnumType.STRING)
    private Especie especie;

    private String raca;

    @NotNull(message = "Porte é obrigatório")
    @Enumerated(EnumType.STRING)
    private Porte porte;

    /** Curta, média, longa, dupla... Ajuda a estimar o tempo de tosa. */
    private String pelagem;
    private String alergias;

    /** Temperamento, medicamentos, vacinas, cuidados na hora do banho. */
    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Builder.Default
    private Boolean ativo = true;

    public enum Especie { CACHORRO, GATO, OUTRO }

    public enum Porte { PEQUENO, MEDIO, GRANDE }
}
