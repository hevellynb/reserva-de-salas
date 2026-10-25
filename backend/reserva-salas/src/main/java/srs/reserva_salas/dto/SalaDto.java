package srs.reserva_salas.dto;

import srs.reserva_salas.domain.Sala;
import srs.reserva_salas.domain.StatusSala;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SalaDto {

    private static final String OBRIGATORIO = "O preenchimento dos campos obrigatórios é necessário.";

    private SalaDto() {
    }

    /** O campo "status" só é usado na edição (para reativar/desativar); no cadastro é ignorado. */
    public record Request(
            @NotBlank(message = OBRIGATORIO) @Size(max = 100) String nome,
            @NotBlank(message = OBRIGATORIO) @Size(max = 150) String localidade,
            @NotNull(message = OBRIGATORIO)
            @Positive(message = "A capacidade de pessoas deve ser configurada com um número superior a 0.")
            Integer capacidade,
            List<@NotBlank @Size(max = 100) String> recursos,
            StatusSala status) {
    }

    /** codigoQr só é preenchido para administradores: ele prova a presença física na sala. */
    public record Response(UUID id, String nome, String localidade, Integer capacidade,
                           List<String> recursos, StatusSala status, String codigoQr) {

        public static Response from(Sala s, boolean incluirCodigoQr) {
            return new Response(s.getId(), s.getNome(), s.getLocalidade(), s.getCapacidade(),
                    new ArrayList<>(s.getRecursos()), s.getStatus(), incluirCodigoQr ? s.getCodigoQr() : null);
        }
    }
}
