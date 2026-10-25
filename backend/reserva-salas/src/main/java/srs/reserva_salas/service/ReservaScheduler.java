package srs.reserva_salas.service;

import srs.reserva_salas.domain.Reserva;
import srs.reserva_salas.domain.StatusReserva;
import srs.reserva_salas.repository.ReservaRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReservaScheduler {

    private final ReservaRepository reservas;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public ReservaScheduler(ReservaRepository reservas, AuditoriaService auditoria, Clock clock) {
        this.reservas = reservas;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    /** A cada minuto cancela as reservas CONFIRMADAS cuja janela de check-in já terminou. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelarPorAusencia() {
        Instant agora = Instant.now(clock);
        Instant limite = agora.minus(Reserva.CHECKIN_DEPOIS);
        for (Reserva reserva : reservas.findByStatusAndInicioLessThan(StatusReserva.CONFIRMADA, limite)) {
            reserva.cancelarPorAusencia(agora);
            auditoria.registrar(null, "RESERVA_CANCELADA_AUSENCIA", "Reserva", reserva.getId(), null);
        }
    }
}
