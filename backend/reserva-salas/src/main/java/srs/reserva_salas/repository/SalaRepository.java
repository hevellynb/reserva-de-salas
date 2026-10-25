package srs.reserva_salas.repository;

import srs.reserva_salas.domain.Sala;
import srs.reserva_salas.domain.StatusReserva;
import srs.reserva_salas.domain.StatusSala;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalaRepository extends JpaRepository<Sala, UUID> {

    List<Sala> findAllByOrderByNomeAsc();

    Optional<Sala> findByCodigoQr(String codigoQr);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    boolean existsByLocalidadeNormalizadaAndStatus(String localidadeNormalizada, StatusSala status);

    boolean existsByLocalidadeNormalizadaAndStatusAndIdNot(String localidadeNormalizada, StatusSala status, UUID id);

    @Query("""
            select s from Sala s
            where s.status = :ativa
              and not exists (
                select r.id from Reserva r
                where r.sala = s
                  and r.status in :bloqueantes
                  and r.inicio < :fim
                  and r.fim > :inicio)
            order by s.nome
            """)
    List<Sala> buscarDisponiveis(@Param("ativa") StatusSala ativa,
                                 @Param("bloqueantes") Collection<StatusReserva> bloqueantes,
                                 @Param("inicio") Instant inicio,
                                 @Param("fim") Instant fim);
}
