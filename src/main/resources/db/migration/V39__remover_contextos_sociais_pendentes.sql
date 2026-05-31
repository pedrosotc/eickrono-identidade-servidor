ALTER TABLE IF EXISTS autenticacao.cadastros_conta
    DROP COLUMN IF EXISTS vinculo_social_pendente_provedor,
    DROP COLUMN IF EXISTS vinculo_social_pendente_identificador_externo,
    DROP COLUMN IF EXISTS vinculo_social_pendente_nome_usuario_externo;

DROP TABLE IF EXISTS autenticacao.contextos_sociais_pendentes;
