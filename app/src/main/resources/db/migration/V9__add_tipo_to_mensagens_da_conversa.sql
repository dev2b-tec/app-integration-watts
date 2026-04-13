-- V9: Adiciona tipo, nome_arquivo, latitude, longitude à mensagens_da_conversa
-- Permite diferenciar texto, imagem, vídeo, documento, áudio PTT, sticker e localização.

ALTER TABLE mensagens_da_conversa
    ADD COLUMN IF NOT EXISTS tipo        VARCHAR(30)   NOT NULL DEFAULT 'TEXTO',
    ADD COLUMN IF NOT EXISTS nome_arquivo VARCHAR(255),
    ADD COLUMN IF NOT EXISTS latitude    DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude   DOUBLE PRECISION;
