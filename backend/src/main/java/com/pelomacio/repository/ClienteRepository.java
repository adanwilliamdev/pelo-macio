package com.pelomacio.repository;

import com.pelomacio.model.Cliente;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    /**
     * Busca por id como consulta (JPQL), e não via EntityManager.find: consultas sempre recebem o filtro de loja
     * do Hibernate, então um id de outra loja simplesmente não é encontrado.
     */
    @Query("select x from Cliente x where x.id = :id")
    Optional<Cliente> findById(@Param("id") Long id);

    List<Cliente> findByNomeContainingIgnoreCaseOrTelefoneContaining(String nome, String telefone, Sort sort);
    long countByAtivoTrue();
    Optional<Cliente> findByUsuarioId(Long usuarioId);
    Optional<Cliente> findFirstByTelefone(String telefone);
}
