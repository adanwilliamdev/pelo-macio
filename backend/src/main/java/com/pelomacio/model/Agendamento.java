package com.pelomacio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Agendamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @TenantId
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    /** Tutor. */
    @ManyToOne(optional = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    private Pet pet;

    @ManyToOne(optional = false)
    private Profissional profissional;

    @ManyToOne(optional = false)
    private Servico servico;

    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.AGENDADO;

    /** Permite atendimento simultâneo com outro já marcado para o mesmo profissional. */
    @Builder.Default
    private Boolean encaixe = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Origem origem = Origem.INTERNA;

    @Builder.Default
    private Boolean lembreteEnviado = false;
    private String googleEventId;

    public enum Status { AGENDADO, CONFIRMADO, CONCLUIDO, CANCELADO, FALTOU }

    /** INTERNA: marcado pela equipe. ONLINE: marcado pelo tutor na página pública. */
    public enum Origem { INTERNA, ONLINE }
}
