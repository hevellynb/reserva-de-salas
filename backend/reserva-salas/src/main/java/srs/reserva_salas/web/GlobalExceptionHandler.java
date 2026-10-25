package srs.reserva_salas.web;

import srs.reserva_salas.exception.ApiException;
import java.time.Instant;
import java.util.List;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResposta> handleApi(ApiException ex) {
        return resposta(ex.getStatus(), ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> handleValidacao(MethodArgumentNotValidException ex) {
        List<String> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Corrija os campos indicados.", erros);
    }

    /** Última barreira: as constraints do banco (sobreposição, nome, localidade, e-mail) caem aqui. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> handleIntegridade(DataIntegrityViolationException ex) {
        String causa = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        String texto = causa == null ? "" : causa;

        String mensagem;
        if (texto.contains("ex_reserva_sem_sobreposicao")) {
            mensagem = "A sala selecionada acabou de ser ocupada. Por favor, selecione outro horário ou sala.";
        } else if (texto.contains("ux_sala_localidade_ativa")) {
            mensagem = "Já existe uma sala ativa nesta localidade.";
        } else if (texto.contains("ux_sala_nome")) {
            mensagem = "Já existe uma sala cadastrada com este nome. Por favor, escolha outro identificador.";
        } else if (texto.contains("ux_usuario_email")) {
            mensagem = "Já existe um usuário cadastrado com este e-mail.";
        } else {
            mensagem = "Conflito de dados. Verifique as informações enviadas.";
        }
        return resposta(HttpStatus.CONFLICT, mensagem, List.of());
    }

    private ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem, List<String> erros) {
        return ResponseEntity.status(status)
                .body(new ErroResposta(Instant.now(), status.value(), mensagem, erros));
    }
}
