package com.pelomacio.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Uma loja (tenant). Não é filtrada por loja: é ela que define o que é "a loja". */
@Entity
@Table(name = "lojas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Loja {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Size(max = 120)
    private String nome;

    /** Identifica a loja na URL pública de agendamento (/agendar/{slug}). */
    private String slug;

    private String telefone;

    @Builder.Default
    private LocalTime abertura = LocalTime.of(9, 0);

    @Builder.Default
    private LocalTime fechamento = LocalTime.of(18, 0);

    /** Espaçamento entre os horários oferecidos na página pública. */
    @Builder.Default
    private Integer intervaloMinutos = 30;

    /** Dias abertos no padrão ISO (1 = segunda ... 7 = domingo), separados por vírgula. */
    @Builder.Default
    private String diasFuncionamento = "1,2,3,4,5,6";

    @Builder.Default
    private Boolean agendamentoOnline = true;

    @Builder.Default
    private LocalDateTime criadaEm = LocalDateTime.now();

    public Set<DayOfWeek> diasAbertos() {
        if (diasFuncionamento == null || diasFuncionamento.isBlank()) return Set.of();
        return Arrays.stream(diasFuncionamento.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> DayOfWeek.of(Integer.parseInt(s)))
                .collect(Collectors.toSet());
    }
}
