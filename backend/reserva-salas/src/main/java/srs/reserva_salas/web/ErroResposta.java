package srs.reserva_salas.web;

import java.time.Instant;
import java.util.List;

public record ErroResposta(Instant timestamp, int status, String mensagem, List<String> erros) {
}
