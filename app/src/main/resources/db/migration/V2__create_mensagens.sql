CREATE TABLE mensagens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instancia_id    UUID         NOT NULL REFERENCES instancias(id) ON DELETE CASCADE,
    remote_jid      VARCHAR(100) NOT NULL,
    numero          VARCHAR(30),
    push_name       VARCHAR(255),
    message_id      VARCHAR(255),
    tipo            VARCHAR(50)  NOT NULL DEFAULT 'TEXTO',
    conteudo        TEXT,
    direcao         VARCHAR(20)  NOT NULL DEFAULT 'RECEBIDA',
    status_envio    VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mensagens_instancia_id ON mensagens(instancia_id);
CREATE INDEX idx_mensagens_remote_jid   ON mensagens(remote_jid);
CREATE INDEX idx_mensagens_direcao      ON mensagens(direcao);
CREATE INDEX idx_mensagens_created_at   ON mensagens(created_at DESC);
