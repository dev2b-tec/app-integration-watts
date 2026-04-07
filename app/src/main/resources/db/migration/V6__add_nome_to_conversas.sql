-- Nome/apelido da pessoa com quem estamos conversando (ex: pushName do WhatsApp).
-- Nullable: conversas antigas e criadas sem nome ficam com NULL.
ALTER TABLE conversas
    ADD COLUMN nome VARCHAR(150);
