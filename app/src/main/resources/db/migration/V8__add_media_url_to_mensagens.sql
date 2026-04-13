-- V8: Adiciona coluna media_url à tabela mensagens
-- Usada para armazenar URLs de imagens, vídeos, documentos, áudios e stickers.

ALTER TABLE mensagens ADD COLUMN IF NOT EXISTS media_url TEXT;
