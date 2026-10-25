package srs.reserva_salas.domain;

import srs.reserva_salas.exception.ApiException;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "reserva")
public class Reserva {

    public static final Duration CHECKIN_ANTES = Duration.ofMinutes(15);
    public static final Duration CHECKIN_DEPOIS = Duration.ofMinutes(10);
    public static final Duration CANCELAMENTO_ANTECEDENCIA = Duration.ofMinutes(30);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id")
    private Sala sala;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusReserva status;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "check_in_em")
    private Instant checkInEm;

    @Column(name = "check_out_em")
    private Instant checkOutEm;

    @Column(name = "cancelado_em")
    private Instant canceladoEm;

    protected Reserva() {
    }

    public Reserva(Sala sala, Usuario usuario, Instant inicio, Instant fim, Instant criadoEm) {
        this.sala = sala;
        this.usuario = usuario;
        this.inicio = inicio;
        this.fim = fim;
        this.criadoEm = criadoEm;
        this.status = StatusReserva.CONFIRMADA;
    }

    public boolean estaNoPeriodoDeCheckIn(Instant agora) {
        return !agora.isBefore(inicio.minus(CHECKIN_ANTES)) && !agora.isAfter(inicio.plus(CHECKIN_DEPOIS));
    }

    /** RN005: só CONFIRMADA e até 30 minutos antes do início. */
    public void cancelarPeloUsuario(Instant agora) {
        if (status != StatusReserva.CONFIRMADA) {
            throw regra("Somente reservas confirmadas podem ser canceladas.");
        }
        if (agora.isAfter(inicio.minus(CANCELAMENTO_ANTECEDENCIA))) {
            throw regra("O cancelamento só é permitido até 30 minutos antes do início da reserva.");
        }
        this.status = StatusReserva.CANCELADA_PELO_USUARIO;
        this.canceladoEm = agora;
    }

    /** RN004: janela de check-in de 15 min antes até 10 min depois do início. */
    public void realizarCheckIn(Instant agora) {
        if (status != StatusReserva.CONFIRMADA) {
            throw regra("Somente reservas confirmadas permitem check-in.");
        }
        if (!estaNoPeriodoDeCheckIn(agora)) {
            throw regra("O check-in só é permitido de 15 minutos antes até 10 minutos após o início da reserva.");
        }
        this.status = StatusReserva.EM_USO;
        this.checkInEm = agora;
    }

    public void confirmarSaida(Instant agora) {
        if (status != StatusReserva.EM_USO) {
            throw regra("Somente reservas em uso podem ter a saída confirmada.");
        }
        this.status = StatusReserva.CONCLUIDA;
        this.checkOutEm = agora;
    }

    public void cancelarPorAusencia(Instant agora) {
        if (status != StatusReserva.CONFIRMADA) {
            throw regra("Somente reservas confirmadas podem ser canceladas por ausência.");
        }
        this.status = StatusReserva.CANCELADA_POR_AUSENCIA;
        this.canceladoEm = agora;
    }

    private static ApiException regra(String mensagem) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, mensagem);
    }

    public UUID getId() { return id; }
    public Sala getSala() { return sala; }
    public Usuario getUsuario() { return usuario; }
    public Instant getInicio() { return inicio; }
    public Instant getFim() { return fim; }
    public StatusReserva getStatus() { return status; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getCheckInEm() { return checkInEm; }
    public Instant getCheckOutEm() { return checkOutEm; }
    public Instant getCanceladoEm() { return canceladoEm; }
}
