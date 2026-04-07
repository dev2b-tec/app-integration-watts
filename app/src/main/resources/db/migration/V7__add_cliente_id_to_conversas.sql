-- ID do cliente cadastrado no sistema vinculado a esta conversa (nullable).
ALTER TABLE conversas
    ADD COLUMN cliente_id UUID;
