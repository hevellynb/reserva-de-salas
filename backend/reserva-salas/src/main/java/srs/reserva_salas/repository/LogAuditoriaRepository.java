package srs.reserva_salas.repository;

import srs.reserva_salas.domain.LogAuditoria;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID> {
}
