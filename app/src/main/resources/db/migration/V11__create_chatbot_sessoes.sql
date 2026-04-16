CREATE TABLE chatbot_sessoes (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    empresa_id      UUID         NOT NULL,
    instance_name   VARCHAR(100) NOT NULL,
    telefone        VARCHAR(20)  NOT NULL,
    fluxo_id        UUID         NOT NULL REFERENCES chatbot_fluxos(id) ON DELETE CASCADE,
    no_atual_id     VARCHAR(100) NOT NULL,
    aguardando_input BOOLEAN      NOT NULL DEFAULT false,
    iniciado_em     TIMESTAMP    NOT NULL,
    atualizado_em   TIMESTAMP    NOT NULL,
    UNIQUE (instance_name, telefone)
);

CREATE INDEX idx_chatbot_sessoes_empresa ON chatbot_sessoes (empresa_id);
