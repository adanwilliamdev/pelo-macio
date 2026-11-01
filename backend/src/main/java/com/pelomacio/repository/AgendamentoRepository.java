package com.pelomacio.repository;

import com.pelomacio.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    /**
     * Busca por id como consulta (JPQL), e não via EntityManager.find: consultas sempre recebem o filtro de loja
     * do Hibernate, então um id de outra loja simplesmente não é encontrado.
     */
    @Query("select x from Agendamento x where x.id = :id")
    Optional<Agendamento> findById(@Param("id") Long id);


    List<Agendamento> findByDataHoraInicioBetweenOrderByDataHoraInicio(LocalDateTime de, LocalDateTime ate);

    long countByDataHoraInicioBetweenAndStatusNotIn(LocalDateTime de, LocalDateTime ate, Collection<Agendamento.Status> status);

    List<Agendamento> findByDataHoraInicioBetweenAndStatusInAndLembreteEnviadoFalse(
            LocalDateTime de, LocalDateTime ate, Collection<Agendamento.Status> status);

    @Query("""
           select count(a) from Agendamento a
           where a.profissional.id = :profissionalId
             and a.status not in :inativos
             and a.dataHoraInicio < :fim
             and a.dataHoraFim > :inicio
             and a.id <> :ignorarId
           """)
    long contarConflitos(@Param("profissionalId") Long profissionalId,
                         @Param("inicio") LocalDateTime inicio,
                         @Param("fim") LocalDateTime fim,
                         @Param("inativos") Collection<Agendamento.Status> inativos,
                         @Param("ignorarId") Long ignorarId);

    /** Agendamentos futuros e ativos da profissional que ainda não viraram evento no Google Calendar. */
    @Query("""
           select a from Agendamento a
           where a.profissional.id = :profissionalId
             and a.googleEventId is null
             and a.status in :ativos
             and a.dataHoraFim > :agora
           order by a.dataHoraInicio
           """)
    List<Agendamento> pendentesGoogle(@Param("profissionalId") Long profissionalId,
                                      @Param("ativos") Collection<Agendamento.Status> ativos,
                                      @Param("agora") LocalDateTime agora);
}
