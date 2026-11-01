package com.pelomacio.repository;

import com.pelomacio.model.Pet;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {

    /** Ver o comentário em {@link ClienteRepository#findById}. */
    @Query("select x from Pet x where x.id = :id")
    Optional<Pet> findById(@Param("id") Long id);

    List<Pet> findByClienteId(Long clienteId, Sort sort);

    List<Pet> findByNomeContainingIgnoreCase(String nome, Sort sort);

    Optional<Pet> findFirstByClienteIdAndNomeIgnoreCase(Long clienteId, String nome);
}
