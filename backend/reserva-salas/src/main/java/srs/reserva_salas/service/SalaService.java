package srs.reserva_salas.service;

import srs.reserva_salas.domain.Sala;
import srs.reserva_salas.domain.StatusReserva;
import srs.reserva_salas.domain.StatusSala;
import srs.reserva_salas.dto.SalaDto;
import srs.reserva_salas.exception.ApiException;
import srs.reserva_salas.repository.ReservaRepository;
import srs.reserva_salas.repository.SalaRepository;
import srs.reserva_salas.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaService {

    private static final String MSG_NOME_DUPLICADO =
            "Já existe uma sala cadastrada com este nome. Por favor, escolha outro identificador.";
    private static final String MSG_LOCALIDADE_DUPLICADA = "Já existe uma sala ativa nesta localidade.";
    private static final String MSG_RESERVAS_FUTURAS =
            "Não é possível desativar a sala diretamente. Existem reservas futuras confirmadas. "
                    + "Realize a gestão ou cancelamento destas reservas antes de prosseguir.";

    private final SalaRepository salas;
    private final ReservaRepository reservas;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public SalaService(SalaRepository salas, ReservaRepository reservas, UsuarioRepository usuarios,
                       AuditoriaService auditoria, Clock clock) {
        this.salas = salas;
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SalaDto.Response> listar(boolean administrador) {
        return salas.findAllByOrderByNomeAsc().stream()
                .filter(s -> administrador || s.isAtiva())
                .map(s -> SalaDto.Response.from(s, administrador))
                .toList();
    }

    @Transactional(readOnly = true)
    public SalaDto.Response buscar(UUID id, boolean administrador) {
        Sala sala = buscarEntidade(id);
        if (!administrador && !sala.isAtiva()) {
            throw naoEncontrada();
        }
        return SalaDto.Response.from(sala, administrador);
    }

    /** RF003 */
    @Transactional(readOnly = true)
    public List<SalaDto.Response> disponiveis(LocalDate data, LocalTime inicio, LocalTime fim) {
        if (data.isBefore(LocalDate.now(clock))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A data de consulta não pode ser inferior à data de hoje.");
        }
        if (!fim.isAfter(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O horário de término deve ser posterior ao horário de início.");
        }
        Instant de = data.atTime(inicio).atZone(clock.getZone()).toInstant();
        Instant ate = data.atTime(fim).atZone(clock.getZone()).toInstant();
        return salas.buscarDisponiveis(StatusSala.ATIVA, StatusReserva.BLOQUEANTES, de, ate).stream()
                .map(s -> SalaDto.Response.from(s, false))
                .toList();
    }

    /** RF007 */
    @Transactional
    public SalaDto.Response criar(UUID adminId, SalaDto.Request req) {
        String nome = req.nome().trim();
        String localidade = req.localidade().trim();

        if (salas.existsByNomeIgnoreCase(nome)) {
            throw new ApiException(HttpStatus.CONFLICT, MSG_NOME_DUPLICADO);
        }
        if (salas.existsByLocalidadeNormalizadaAndStatus(Sala.normalizarLocalidade(localidade), StatusSala.ATIVA)) {
            throw new ApiException(HttpStatus.CONFLICT, MSG_LOCALIDADE_DUPLICADA);
        }

        Sala sala = salas.save(new Sala(nome, localidade, req.capacidade(), req.recursos()));
        auditoria.registrar(usuarios.getReferenceById(adminId), "SALA_CRIADA", "Sala", sala.getId(), "nome=" + nome);
        return SalaDto.Response.from(sala, true);
    }

    /** RF008 (também permite reativar ou desativar enviando "status"). */
    @Transactional
    public SalaDto.Response editar(UUID adminId, UUID id, SalaDto.Request req) {
        Sala sala = buscarEntidade(id);
        String nome = req.nome().trim();
        String localidade = req.localidade().trim();
        StatusSala novoStatus = req.status() != null ? req.status() : sala.getStatus();

        if (salas.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new ApiException(HttpStatus.CONFLICT, MSG_NOME_DUPLICADO);
        }
        if (novoStatus == StatusSala.ATIVA && salas.existsByLocalidadeNormalizadaAndStatusAndIdNot(
                Sala.normalizarLocalidade(localidade), StatusSala.ATIVA, id)) {
            throw new ApiException(HttpStatus.CONFLICT, MSG_LOCALIDADE_DUPLICADA);
        }
        if (sala.isAtiva() && novoStatus == StatusSala.INATIVA) {
            verificarSemReservasFuturas(sala);
        }

        sala.setNome(nome);
        sala.setLocalidade(localidade);
        sala.setCapacidade(req.capacidade());
        sala.substituirRecursos(req.recursos());
        if (novoStatus == StatusSala.ATIVA) {
            sala.ativar();
        } else {
            sala.desativar();
        }

        auditoria.registrar(usuarios.getReferenceById(adminId), "SALA_EDITADA", "Sala", sala.getId(), "nome=" + nome);
        return SalaDto.Response.from(sala, true);
    }

    /** RF009 / RF010: desativa preservando todo o histórico (nada é apagado). */
    @Transactional
    public SalaDto.Response desativar(UUID adminId, UUID id) {
        Sala sala = buscarEntidade(id);
        if (!sala.isAtiva()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "A sala já está inativa.");
        }
        verificarSemReservasFuturas(sala);
        sala.desativar();
        auditoria.registrar(usuarios.getReferenceById(adminId), "SALA_DESATIVADA", "Sala", sala.getId(),
                "nome=" + sala.getNome());
        return SalaDto.Response.from(sala, true);
    }

    private void verificarSemReservasFuturas(Sala sala) {
        if (reservas.existsBySalaIdAndStatusAndFimGreaterThan(
                sala.getId(), StatusReserva.CONFIRMADA, Instant.now(clock))) {
            throw new ApiException(HttpStatus.CONFLICT, MSG_RESERVAS_FUTURAS);
        }
    }

    private Sala buscarEntidade(UUID id) {
        return salas.findById(id).orElseThrow(SalaService::naoEncontrada);
    }

    private static ApiException naoEncontrada() {
        return new ApiException(HttpStatus.NOT_FOUND, "Sala não encontrada.");
    }
}
