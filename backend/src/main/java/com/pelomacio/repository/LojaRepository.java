package com.pelomacio.repository;

import com.pelomacio.model.Loja;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** Lojas não são filtradas por loja: é por aqui que se descobre qual é a loja. */
public interface LojaRepository extends JpaRepository<Loja, Long> {
    Optional<Loja> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
