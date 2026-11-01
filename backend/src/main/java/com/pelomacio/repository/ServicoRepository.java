package com.pelomacio.repository;

import com.pelomacio.model.Servico;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
    /**
     * Busca por id como consulta (JPQL), e não via EntityManager.find: consultas sempre recebem o filtro de loja
     * do Hibernate, então um id de outra loja simplesmente não é encontrado.
     */
    @Query("select x from Servico x where x.id = :id")
    Optional<Servico> findById(@Param("id") Long id);

    List<Servico> findByNomeContainingIgnoreCase(String nome, Sort sort);
}
