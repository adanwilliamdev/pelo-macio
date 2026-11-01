package com.pelomacio.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Vínculo OAuth2 entre um profissional e a conta Google dele. Não é serializado pela API. */
@Entity
@Table(name = "google_conexoes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoogleConexao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "profissional_id")
    private Profissional profissional;

    /** E-mail da conta Google conectada (informativo). */
    private String email;

    /** Refresh token cifrado com {@code TokenCipher}. */
    private String refreshToken;

    /** True quando o Google recusou o token (acesso revogado): a profissional precisa conectar de novo. */
    @Builder.Default
    private Boolean reautorizar = false;

    private LocalDateTime conectadoEm;
}
