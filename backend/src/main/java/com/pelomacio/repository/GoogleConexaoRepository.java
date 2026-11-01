package com.pelomacio.repository;

import com.pelomacio.model.GoogleConexao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GoogleConexaoRepository extends JpaRepository<GoogleConexao, Long> {
    Optional<GoogleConexao> findByProfissionalId(Long profissionalId);

    List<GoogleConexao> findByProfissionalIdIn(Collection<Long> profissionalIds);
}
