package srs.reserva_salas.repository;

import srs.reserva_salas.domain.Reserva;
import srs.reserva_salas.domain.StatusReserva;
import srs.reserva_salas.dto.ContagemItem;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    @EntityGraph(attributePaths = "sala")
    List<Reserva> findByUsuarioIdOrderByInicioDesc(UUID usuarioId);

    List<Reserva> findByUsuarioIdAndSalaIdAndStatus(UUID usuarioId, UUID salaId, StatusReserva status);

    List<Reserva> findByUsuarioIdAndStatusInAndInicioGreaterThanEqualAndInicioLessThan(
            UUID usuarioId, Collection<StatusReserva> status, Instant de, Instant ate);

    List<Reserva> findByStatusAndInicioLessThan(StatusReserva status, Instant limite);

    boolean existsBySalaIdAndStatusAndFimGreaterThan(UUID salaId, StatusReserva status, Instant agora);

    @Query("""
            select count(r) from Reserva r
            where r.sala.id = :salaId
              and r.status in :bloqueantes
              and r.inicio < :fim
              and r.fim > :inicio
            """)
    long contarSobrepostas(@Param("salaId") UUID salaId,
                           @Param("bloqueantes") Collection<StatusReserva> bloqueantes,
                           @Param("inicio") Instant inicio,
                           @Param("fim") Instant fim);

    @Query("""
            select new srs.reserva_salas.dto.ContagemItem(s.nome, count(r))
            from Reserva r join r.sala s
            where r.inicio >= :de and r.inicio < :ate
            group by s.id, s.nome
            order by count(r) desc, s.nome
            """)
    List<ContagemItem> volumePorSala(@Param("de") Instant de, @Param("ate") Instant ate);

    @Query("""
            select new srs.reserva_salas.dto.ContagemItem(u.nome, count(r))
            from Reserva r join r.usuario u
            where r.inicio >= :de and r.inicio < :ate
            group by u.id, u.nome
            order by count(r) desc, u.nome
            """)
    List<ContagemItem> volumePorUsuario(@Param("de") Instant de, @Param("ate") Instant ate);
}
