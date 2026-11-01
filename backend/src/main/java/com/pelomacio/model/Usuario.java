package com.pelomacio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Loja do usuário. Usuários são globais (login por e-mail), por isso não usam o filtro automático. */
    @Column(name = "loja_id", nullable = false, updatable = false)
    private Long lojaId;

    private String nome;
    private String email;

    @JsonIgnore
    private String senha;
    private String telefone;

    /** Usuário inativo não consegue entrar e tem o token atual rejeitado. */
    @Builder.Default
    private Boolean ativo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_perfis", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "perfil")
    @Enumerated(EnumType.STRING)
    private Set<Perfil> perfis;

    public enum Perfil { ADMINISTRADOR, PROFISSIONAL, CLIENTE }
}
