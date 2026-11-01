package com.pelomacio.repository;

import com.pelomacio.dto.ComissaoResumo;
import com.pelomacio.model.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {
    /**
     * Busca por id como consulta (JPQL), e não via EntityManager.find: consultas sempre recebem o filtro de loja
     * do Hibernate, então um id de outra loja simplesmente não é encontrado.
     */
    @Query("select x from Lancamento x where x.id = :id")
    Optional<Lancamento> findById(@Param("id") Long id);


    List<Lancamento> findByDataBetweenOrderByDataDescIdDesc(LocalDate de, LocalDate ate);

    @Query("select coalesce(sum(l.valor), 0) from Lancamento l where l.tipo = :tipo and l.data between :de and :ate")
    BigDecimal somar(@Param("tipo") Lancamento.Tipo tipo, @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    @Query("""
           select new com.pelomacio.dto.ComissaoResumo(l.profissional.id, l.profissional.nome, sum(l.valor))
           from Lancamento l
           where l.categoria = 'COMISSAO' and l.profissional is not null and l.data between :de and :ate
           group by l.profissional.id, l.profissional.nome
           order by l.profissional.nome
           """)
    List<ComissaoResumo> comissoesPorProfissional(@Param("de") LocalDate de, @Param("ate") LocalDate ate);
}
