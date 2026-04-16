ALTER TABLE chatbot_sessoes
    ADD COLUMN variaveis JSONB NOT NULL DEFAULT '{}';
