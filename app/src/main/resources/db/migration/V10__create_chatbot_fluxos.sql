CREATE TABLE chatbot_fluxos (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    empresa_id    UUID         NOT NULL,
    nome          VARCHAR(100) NOT NULL,
    descricao     TEXT,
    ativo         BOOLEAN      NOT NULL DEFAULT false,
    fluxo         JSONB        NOT NULL DEFAULT '{"nodes":[],"edges":[]}',
    criado_em     TIMESTAMP    NOT NULL,
    atualizado_em TIMESTAMP    NOT NULL
);

CREATE INDEX idx_chatbot_fluxos_empresa ON chatbot_fluxos (empresa_id);
