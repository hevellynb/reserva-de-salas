package srs.reserva_salas.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sala")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 150)
    private String localidade;

    /** Localidade em minúsculas e sem espaços excedentes, usada na regra RN007. */
    @Column(name = "localidade_normalizada", nullable = false, length = 150)
    private String localidadeNormalizada;

    @Column(nullable = false)
    private Integer capacidade;

    @ElementCollection
    @CollectionTable(name = "sala_recurso", joinColumns = @JoinColumn(name = "sala_id"))
    @Column(name = "recurso", nullable = false, length = 100)
    private List<String> recursos = new ArrayList<>();

    @Column(name = "codigo_qr", nullable = false, length = 64, unique = true)
    private String codigoQr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusSala status = StatusSala.ATIVA;

    protected Sala() {
    }

    public Sala(String nome, String localidade, Integer capacidade, List<String> recursos) {
        this.nome = nome;
        setLocalidade(localidade);
        this.capacidade = capacidade;
        substituirRecursos(recursos);
        this.codigoQr = UUID.randomUUID().toString().replace("-", "");
    }

    public static String normalizarLocalidade(String localidade) {
        return localidade.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public void setLocalidade(String localidade) {
        this.localidade = localidade;
        this.localidadeNormalizada = normalizarLocalidade(localidade);
    }

    public void substituirRecursos(List<String> novos) {
        this.recursos.clear();
        if (novos != null) {
            novos.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .forEach(this.recursos::add);
        }
    }

    public boolean isAtiva() {
        return status == StatusSala.ATIVA;
    }

    public void ativar() {
        this.status = StatusSala.ATIVA;
    }

    public void desativar() {
        this.status = StatusSala.INATIVA;
    }

    public void setNome(String nome) { this.nome = nome; }
    public void setCapacidade(Integer capacidade) { this.capacidade = capacidade; }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getLocalidade() { return localidade; }
    public Integer getCapacidade() { return capacidade; }
    public List<String> getRecursos() { return recursos; }
    public String getCodigoQr() { return codigoQr; }
    public StatusSala getStatus() { return status; }
}
