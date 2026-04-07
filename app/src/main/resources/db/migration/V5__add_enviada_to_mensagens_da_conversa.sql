-- Indica se a mensagem foi enviada por nós (true) ou recebida do contato (false).
-- Registros anteriores à migração são tratados como recebidos (false).
ALTER TABLE mensagens_da_conversa
    ADD COLUMN enviada BOOLEAN NOT NULL DEFAULT FALSE;
