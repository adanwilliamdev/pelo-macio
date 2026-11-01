package com.pelomacio.repository;

import com.pelomacio.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findByLojaId(Long lojaId, Sort sort);

    /** Usuários são globais; por isso o filtro de loja é explícito aqui. */
    @Query("select count(u) from Usuario u where u.lojaId = :lojaId and u.ativo = true and :perfil member of u.perfis")
    long contarAtivosComPerfil(@Param("perfil") Usuario.Perfil perfil, @Param("lojaId") Long lojaId);
}
