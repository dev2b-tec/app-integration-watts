CREATE TABLE instancias (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id          UUID NOT NULL,
    usuario_id          UUID,
    nome                VARCHAR(255) NOT NULL,
    instance_name       VARCHAR(100) NOT NULL UNIQUE,
    status              VARCHAR(30)  NOT NULL DEFAULT 'CRIADA',
    numero              VARCHAR(30),
    qr_code             TEXT,
    webhook_configurado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_instancias_empresa_id   ON instancias(empresa_id);
CREATE INDEX idx_instancias_usuario_id   ON instancias(usuario_id);
CREATE INDEX idx_instancias_status       ON instancias(status);
CREATE INDEX idx_instancias_instance_name ON instancias(instance_name);
