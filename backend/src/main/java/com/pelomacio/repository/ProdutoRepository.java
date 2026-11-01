package com.pelomacio.repository;

import com.pelomacio.model.Produto;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    /**
     * Busca por id como consulta (JPQL), e não via EntityManager.find: consultas sempre recebem o filtro de loja
     * do Hibernate, então um id de outra loja simplesmente não é encontrado.
     */
    @Query("select x from Produto x where x.id = :id")
    Optional<Produto> findById(@Param("id") Long id);

    List<Produto> findByNomeContainingIgnoreCase(String nome, Sort sort);

    @Query("select p from Produto p where p.quantidadeEstoque <= p.estoqueMinimo order by p.nome")
    List<Produto> findAbaixoDoMinimo();
}
