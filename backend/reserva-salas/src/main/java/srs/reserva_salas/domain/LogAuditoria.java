package srs.reserva_salas.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "log_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 50)
    private String acao;

    @Column(length = 50)
    private String entidade;

    @Column(name = "entidade_id")
    private UUID entidadeId;

    @Column(length = 500)
    private String detalhes;

    protected LogAuditoria() {
    }

    public LogAuditoria(Instant dataHora, Usuario usuario, String acao, String entidade,
                        UUID entidadeId, String detalhes) {
        this.dataHora = dataHora;
        this.usuario = usuario;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.detalhes = detalhes;
    }
}
