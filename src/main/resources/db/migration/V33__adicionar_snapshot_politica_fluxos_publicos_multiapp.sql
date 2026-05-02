ALTER TABLE autenticacao.cadastros_conta
    ADD COLUMN IF NOT EXISTS locale_solicitante VARCHAR(16),
    ADD COLUMN IF NOT EXISTS time_zone_solicitante VARCHAR(64),
    ADD COLUMN IF NOT EXISTS tipo_produto_exibicao VARCHAR(32),
    ADD COLUMN IF NOT EXISTS produto_exibicao VARCHAR(128),
    ADD COLUMN IF NOT EXISTS canal_exibicao VARCHAR(64),
    ADD COLUMN IF NOT EXISTS empresa_exibicao VARCHAR(128),
    ADD COLUMN IF NOT EXISTS ambiente_exibicao VARCHAR(32),
    ADD COLUMN IF NOT EXISTS exige_validacao_telefone_snapshot BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE autenticacao.recuperacoes_senha
    ADD COLUMN IF NOT EXISTS locale_solicitante VARCHAR(16),
    ADD COLUMN IF NOT EXISTS time_zone_solicitante VARCHAR(64),
    ADD COLUMN IF NOT EXISTS tipo_produto_exibicao VARCHAR(32),
    ADD COLUMN IF NOT EXISTS produto_exibicao VARCHAR(128),
    ADD COLUMN IF NOT EXISTS canal_exibicao VARCHAR(64),
    ADD COLUMN IF NOT EXISTS empresa_exibicao VARCHAR(128),
    ADD COLUMN IF NOT EXISTS ambiente_exibicao VARCHAR(32),
    ADD COLUMN IF NOT EXISTS exige_validacao_telefone_snapshot BOOLEAN NOT NULL DEFAULT FALSE;
