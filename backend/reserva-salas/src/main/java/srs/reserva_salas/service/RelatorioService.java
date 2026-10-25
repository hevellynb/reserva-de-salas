package srs.reserva_salas.service;

import srs.reserva_salas.dto.ContagemItem;
import srs.reserva_salas.exception.ApiException;
import srs.reserva_salas.repository.ReservaRepository;
import srs.reserva_salas.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatorioService {

    private final ReservaRepository reservas;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public RelatorioService(ReservaRepository reservas, UsuarioRepository usuarios,
                            AuditoriaService auditoria, Clock clock) {
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Transactional
    public List<ContagemItem> reservasPorSala(UUID adminId, LocalDate inicio, LocalDate fim) {
        validar(inicio, fim);
        List<ContagemItem> itens = reservas.volumePorSala(inicioDoDia(inicio), inicioDoDia(fim.plusDays(1)));
        auditar(adminId, "reservas-por-sala", inicio, fim);
        return itens;
    }

    @Transactional
    public List<ContagemItem> reservasPorUsuario(UUID adminId, LocalDate inicio, LocalDate fim) {
        validar(inicio, fim);
        List<ContagemItem> itens = reservas.volumePorUsuario(inicioDoDia(inicio), inicioDoDia(fim.plusDays(1)));
        auditar(adminId, "reservas-por-usuario", inicio, fim);
        return itens;
    }

    private void validar(LocalDate inicio, LocalDate fim) {
        if (fim.isBefore(inicio)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Período inválido. A data final deve ser igual ou posterior à data inicial.");
        }
    }

    private Instant inicioDoDia(LocalDate dia) {
        return dia.atStartOfDay(clock.getZone()).toInstant();
    }

    private void auditar(UUID adminId, String tipo, LocalDate inicio, LocalDate fim) {
        auditoria.registrar(usuarios.getReferenceById(adminId), "RELATORIO_EMITIDO", "Relatorio", null,
                "tipo=" + tipo + ", periodo=" + inicio + " a " + fim);
    }
}