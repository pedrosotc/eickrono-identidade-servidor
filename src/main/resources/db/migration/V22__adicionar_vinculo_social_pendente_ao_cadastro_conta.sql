ALTER TABLE cadastros_conta
    ADD COLUMN IF NOT EXISTS vinculo_social_pendente_provedor VARCHAR(32),
    ADD COLUMN IF NOT EXISTS vinculo_social_pendente_identificador_externo VARCHAR(255),
    ADD COLUMN IF NOT EXISTS vinculo_social_pendente_nome_usuario_externo VARCHAR(255);
