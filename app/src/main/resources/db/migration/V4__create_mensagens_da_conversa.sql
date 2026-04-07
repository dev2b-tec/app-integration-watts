CREATE TABLE mensagens_da_conversa
(
    id           UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    conversa_id  UUID        NOT NULL REFERENCES conversas (id) ON DELETE CASCADE,
    texto        TEXT,
    recebida_em  TIMESTAMPTZ NOT NULL,
    url_arquivo  TEXT
);

CREATE INDEX idx_msgconv_conversa_id ON mensagens_da_conversa (conversa_id);
CREATE INDEX idx_msgconv_recebida_em ON mensagens_da_conversa (recebida_em);
