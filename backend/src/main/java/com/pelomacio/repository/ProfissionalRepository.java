package com.pelomacio.repository;

import com.pelomacio.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {
    /**
     * Busca por id como consulta (JPQL), e não via EntityManager.find: consultas sempre recebem o filtro de loja
     * do Hibernate, então um id de outra loja simplesmente não é encontrado.
     */
    @Query("select x from Profissional x where x.id = :id")
    Optional<Profissional> findById(@Param("id") Long id);

    Optional<Profissional> findByUsuarioId(Long usuarioId);
    Optional<Profissional> findByUsuarioEmailIgnoreCase(String email);
}
