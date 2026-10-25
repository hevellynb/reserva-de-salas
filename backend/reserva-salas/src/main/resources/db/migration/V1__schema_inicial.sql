CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuario (
                         id          UUID PRIMARY KEY,
                         nome        VARCHAR(150) NOT NULL,
                         email       VARCHAR(255) NOT NULL,
                         senha_hash  VARCHAR(255) NOT NULL,
                         perfil      VARCHAR(30)  NOT NULL,
                         ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
                         CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('USUARIO_COMUM', 'ADMINISTRADOR'))
);
CREATE UNIQUE INDEX ux_usuario_email ON usuario (lower(email));

CREATE TABLE sala (
                      id                     UUID PRIMARY KEY,
                      nome                   VARCHAR(100) NOT NULL,
                      localidade             VARCHAR(150) NOT NULL,
                      localidade_normalizada VARCHAR(150) NOT NULL,
                      capacidade             INTEGER      NOT NULL,
                      codigo_qr              VARCHAR(64)  NOT NULL,
                      status                 VARCHAR(10)  NOT NULL,
                      CONSTRAINT ck_sala_capacidade CHECK (capacidade > 0),
                      CONSTRAINT ck_sala_status CHECK (status IN ('ATIVA', 'INATIVA')),
                      CONSTRAINT uk_sala_codigo_qr UNIQUE (codigo_qr)
);
CREATE UNIQUE INDEX ux_sala_nome ON sala (lower(nome));
-- RN007: só uma sala ATIVA por localidade (salas inativas podem repetir)
CREATE UNIQUE INDEX ux_sala_localidade_ativa ON sala (localidade_normalizada) WHERE status = 'ATIVA';

CREATE TABLE sala_recurso (
                              sala_id UUID         NOT NULL REFERENCES sala (id),
                              recurso VARCHAR(100) NOT NULL
);
CREATE INDEX ix_sala_recurso_sala ON sala_recurso (sala_id);

CREATE TABLE reserva (
                         id           UUID PRIMARY KEY,
                         sala_id      UUID         NOT NULL REFERENCES sala (id),
                         usuario_id   UUID         NOT NULL REFERENCES usuario (id),
                         inicio       TIMESTAMPTZ  NOT NULL,
                         fim          TIMESTAMPTZ  NOT NULL,
                         status       VARCHAR(30)  NOT NULL,
                         criado_em    TIMESTAMPTZ  NOT NULL,
                         check_in_em  TIMESTAMPTZ,
                         check_out_em TIMESTAMPTZ,
                         cancelado_em TIMESTAMPTZ,
                         CONSTRAINT ck_reserva_periodo CHECK (fim > inicio),
                         CONSTRAINT ck_reserva_status CHECK (status IN
                                                             ('CONFIRMADA', 'EM_USO', 'CONCLUIDA', 'CANCELADA_PELO_USUARIO', 'CANCELADA_POR_AUSENCIA')),
    -- RN001: o próprio banco impede sobreposição de reservas CONFIRMADA/EM_USO na mesma sala
                         CONSTRAINT ex_reserva_sem_sobreposicao EXCLUDE USING gist (
        sala_id WITH =,
        tstzrange(inicio, fim) WITH &&
    ) WHERE (status IN ('CONFIRMADA', 'EM_USO'))
);
CREATE INDEX ix_reserva_usuario_inicio ON reserva (usuario_id, inicio);
CREATE INDEX ix_reserva_sala_inicio ON reserva (sala_id, inicio);
CREATE INDEX ix_reserva_status_inicio ON reserva (status, inicio);

CREATE TABLE log_auditoria (
                               id          UUID PRIMARY KEY,
                               data_hora   TIMESTAMPTZ NOT NULL,
                               usuario_id  UUID REFERENCES usuario (id),
                               acao        VARCHAR(50) NOT NULL,
                               entidade    VARCHAR(50),
                               entidade_id UUID,
                               detalhes    VARCHAR(500)
);
CREATE INDEX ix_log_auditoria_data ON log_auditoria (data_hora);