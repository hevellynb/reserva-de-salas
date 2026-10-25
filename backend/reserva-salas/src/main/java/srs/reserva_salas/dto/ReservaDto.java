package srs.reserva_salas.dto;

import srs.reserva_salas.domain.Reserva;
import srs.reserva_salas.domain.StatusReserva;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ReservaDto {

    private ReservaDto() {
    }

    public record CriarRequest(
            @NotNull UUID salaId,
            @NotNull OffsetDateTime inicio,
            @NotNull OffsetDateTime fim) {
    }

    public record CheckInRequest(@NotBlank String codigoQr) {
    }

    public record Response(UUID id, UUID salaId, String salaNome, Instant inicio, Instant fim,
                           StatusReserva status, Instant criadoEm, Instant checkInEm,
                           Instant checkOutEm, Instant canceladoEm) {

        public static Response from(Reserva r) {
            return new Response(r.getId(), r.getSala().getId(), r.getSala().getNome(), r.getInicio(),
                    r.getFim(), r.getStatus(), r.getCriadoEm(), r.getCheckInEm(),
                    r.getCheckOutEm(), r.getCanceladoEm());
        }
    }
}
