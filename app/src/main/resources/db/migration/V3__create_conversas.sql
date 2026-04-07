CREATE TABLE conversas
(
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    empresa_id UUID        NOT NULL,
    telefone   VARCHAR(30) NOT NULL,
    status     VARCHAR(30) NOT NULL DEFAULT 'ATIVA',
    created_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_conversas_empresa_id ON conversas (empresa_id);
CREATE INDEX idx_conversas_telefone   ON conversas (telefone);
CREATE INDEX idx_conversas_status     ON conversas (status);

-- Garante que cada telefone tenha apenas uma conversa não-finalizada por empresa
CREATE UNIQUE INDEX idx_conversas_empresa_telefone_aberta
    ON conversas (empresa_id, telefone)
    WHERE status <> 'FINALIZADA';
