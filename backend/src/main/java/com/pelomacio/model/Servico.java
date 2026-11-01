package com.pelomacio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "servicos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Servico implements Identificavel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @TenantId
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    private String categoria;

    /** Duração e preço do porte PEQUENO (a base). */
    @NotNull @Min(5)
    private Integer duracaoMinutos;

    @NotNull @DecimalMin("0")
    private BigDecimal preco;

    /** Porte médio e grande: se vazios, valem a duração e o preço base. */
    @Min(5)
    private Integer duracaoMedio;

    @DecimalMin("0")
    private BigDecimal precoMedio;

    @Min(5)
    private Integer duracaoGrande;

    @DecimalMin("0")
    private BigDecimal precoGrande;

    @Builder.Default
    private Boolean ativo = true;

    @Valid
    @Builder.Default
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "servico_id")
    private List<ServicoInsumo> insumos = new ArrayList<>();

    public BigDecimal precoPara(Pet.Porte porte) {
        BigDecimal valor = switch (porte == null ? Pet.Porte.PEQUENO : porte) {
            case PEQUENO -> preco;
            case MEDIO -> precoMedio;
            case GRANDE -> precoGrande;
        };
        return valor != null ? valor : preco;
    }

    public int duracaoPara(Pet.Porte porte) {
        Integer valor = switch (porte == null ? Pet.Porte.PEQUENO : porte) {
            case PEQUENO -> duracaoMinutos;
            case MEDIO -> duracaoMedio;
            case GRANDE -> duracaoGrande;
        };
        return valor != null ? valor : duracaoMinutos;
    }
}
