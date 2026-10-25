package srs.reserva_salas.service;

import srs.reserva_salas.domain.PerfilUsuario;
import srs.reserva_salas.domain.Reserva;
import srs.reserva_salas.domain.Sala;
import srs.reserva_salas.domain.StatusReserva;
import srs.reserva_salas.domain.Usuario;
import srs.reserva_salas.dto.ReservaDto;
import srs.reserva_salas.exception.ApiException;
import srs.reserva_salas.repository.ReservaRepository;
import srs.reserva_salas.repository.SalaRepository;
import srs.reserva_salas.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaService {

    private static final long LIMITE_DIARIO_MINUTOS = 4 * 60;          // RN002
    private static final Duration ANTECEDENCIA_MAXIMA = Duration.ofDays(30); // RN003
    private static final String MSG_CONFLITO =
            "A sala selecionada acabou de ser ocupada. Por favor, selecione outro horário ou sala.";

    private final ReservaRepository reservas;
    private final SalaRepository salas;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public ReservaService(ReservaRepository reservas, SalaRepository salas, UsuarioRepository usuarios,
                          AuditoriaService auditoria, Clock clock) {
        this.reservas = reservas;
        this.salas = salas;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    /** RF002 */
    @Transactional(readOnly = true)
    public List<ReservaDto.Response> minhas(UUID usuarioId) {
        return reservas.findByUsuarioIdOrderByInicioDesc(usuarioId).stream()
                .map(ReservaDto.Response::from)
                .toList();
    }

    /** RF004 */
    @Transactional
    public ReservaDto.Response agendar(UUID usuarioId, ReservaDto.CriarRequest req) {
        Usuario usuario = usuarios.buscarParaAtualizar(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));
        if (!usuario.podeReservar()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Sua conta está inativa. Entre em contato com o administrador.");
        }
        Sala sala = salas.findById(req.salaId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sala não encontrada."));
        if (!sala.isAtiva()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "A sala selecionada está inativa e não aceita novas reservas.");
        }

        Instant agora = Instant.now(clock);
        Instant inicio = req.inicio().toInstant();
        Instant fim = req.fim().toInstant();
        validarPeriodo(inicio, fim, agora);

        if (usuario.getPerfil() == PerfilUsuario.USUARIO_COMUM) {
            validarLimiteDiario(usuarioId, inicio, fim);
        }
        if (reservas.contarSobrepostas(sala.getId(), StatusReserva.BLOQUEANTES, inicio, fim) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, MSG_CONFLITO);
        }

        Reserva reserva = reservas.saveAndFlush(new Reserva(sala, usuario, inicio, fim, agora));
        auditoria.registrar(usuario, "RESERVA_CRIADA", "Reserva", reserva.getId(), "sala=" + sala.getNome());
        return ReservaDto.Response.from(reserva);
    }

    /** RF005 */
    @Transactional
    public ReservaDto.Response cancelar(UUID usuarioId, UUID reservaId) {
        Reserva reserva = buscarDoUsuario(reservaId, usuarioId);
        reserva.cancelarPeloUsuario(Instant.now(clock));
        auditoria.registrar(reserva.getUsuario(), "RESERVA_CANCELADA", "Reserva", reserva.getId(), null);
        return ReservaDto.Response.from(reserva);
    }

    /** RF006: o código QR da sala comprova a presença física. */
    @Transactional
    public ReservaDto.Response checkIn(UUID usuarioId, String codigoQr) {
        Sala sala = salas.findByCodigoQr(codigoQr.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Código de sala inválido."));
        List<Reserva> confirmadas =
                reservas.findByUsuarioIdAndSalaIdAndStatus(usuarioId, sala.getId(), StatusReserva.CONFIRMADA);
        if (confirmadas.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não há agendamento confirmado para esta sala no horário atual.");
        }

        Instant agora = Instant.now(clock);
        Reserva reserva = confirmadas.stream()
                .filter(r -> r.estaNoPeriodoDeCheckIn(agora))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "O check-in só é permitido de 15 minutos antes até 10 minutos após o início da reserva."));

        reserva.realizarCheckIn(agora);
        auditoria.registrar(reserva.getUsuario(), "RESERVA_CHECKIN", "Reserva", reserva.getId(), null);
        return ReservaDto.Response.from(reserva);
    }

    /** RF012 */
    @Transactional
    public ReservaDto.Response confirmarSaida(UUID usuarioId, UUID reservaId) {
        Reserva reserva = buscarDoUsuario(reservaId, usuarioId);
        reserva.confirmarSaida(Instant.now(clock));
        auditoria.registrar(reserva.getUsuario(), "RESERVA_SAIDA", "Reserva", reserva.getId(), null);
        return ReservaDto.Response.from(reserva);
    }

    private void validarPeriodo(Instant inicio, Instant fim, Instant agora) {
        if (!fim.isAfter(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O horário de término deve ser posterior ao horário de início.");
        }
        if (inicio.isBefore(agora)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Não é possível reservar um horário que já começou.");
        }
        if (inicio.isAfter(agora.plus(ANTECEDENCIA_MAXIMA))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Agendamentos só são permitidos com até 30 dias de antecedência.");
        }
        ZoneId zona = clock.getZone();
        LocalDate diaInicio = inicio.atZone(zona).toLocalDate();
        LocalDate diaFim = fim.minusNanos(1).atZone(zona).toLocalDate();
        if (!diaInicio.equals(diaFim)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "A reserva deve começar e terminar no mesmo dia.");
        }
    }

    /** RN002: máximo de 4 horas acumuladas por dia-calendário (CONFIRMADA + EM_USO). */
    private void validarLimiteDiario(UUID usuarioId, Instant inicio, Instant fim) {
        ZoneId zona = clock.getZone();
        LocalDate dia = inicio.atZone(zona).toLocalDate();
        Instant de = dia.atStartOfDay(zona).toInstant();
        Instant ate = dia.plusDays(1).atStartOfDay(zona).toInstant();

        long acumulado = reservas
                .findByUsuarioIdAndStatusInAndInicioGreaterThanEqualAndInicioLessThan(
                        usuarioId, StatusReserva.BLOQUEANTES, de, ate)
                .stream()
                .mapToLong(r -> Duration.between(r.getInicio(), r.getFim()).toMinutes())
                .sum();
        long solicitado = Duration.between(inicio, fim).toMinutes();

        if (acumulado + solicitado > LIMITE_DIARIO_MINUTOS) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A reserva excede o limite permitido de 4 horas diárias por usuário.");
        }
    }

    /** Devolve 404 também quando a reserva é de outra pessoa, para não revelar que ela existe. */
    private Reserva buscarDoUsuario(UUID reservaId, UUID usuarioId) {
        return reservas.findById(reservaId)
                .filter(r -> r.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reserva não encontrada."));
    }
}
