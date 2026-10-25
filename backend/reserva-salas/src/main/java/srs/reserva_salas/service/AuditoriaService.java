package srs.reserva_salas.service;

import srs.reserva_salas.domain.LogAuditoria;
import srs.reserva_salas.domain.Usuario;
import srs.reserva_salas.repository.LogAuditoriaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** RNF007. Nunca passe senhas, tokens ou outros segredos em "detalhes". */
@Service
public class AuditoriaService {

    private final LogAuditoriaRepository repository;
    private final Clock clock;

    public AuditoriaService(LogAuditoriaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void registrar(Usuario usuario, String acao, String entidade, UUID entidadeId, String detalhes) {
        String texto = detalhes != null && detalhes.length() > 500 ? detalhes.substring(0, 500) : detalhes;
        repository.save(new LogAuditoria(Instant.now(clock), usuario, acao, entidade, entidadeId, texto));
    }
}