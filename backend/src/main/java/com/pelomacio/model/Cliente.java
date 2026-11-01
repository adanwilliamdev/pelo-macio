package com.pelomacio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "clientes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cliente implements Identificavel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @TenantId
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    private String telefone;
    private String email;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Builder.Default
    private Boolean ativo = true;
}
