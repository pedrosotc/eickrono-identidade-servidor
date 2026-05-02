ALTER TABLE cadastros_conta
    ADD COLUMN IF NOT EXISTS cliente_ecossistema_id BIGINT,
    ADD COLUMN IF NOT EXISTS exige_validacao_telefone_snapshot BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_cadastros_conta_cliente_ecossistema_id
    ON cadastros_conta (cliente_ecossistema_id);

ALTER TABLE recuperacoes_senha
    ADD COLUMN IF NOT EXISTS cliente_ecossistema_id BIGINT,
    ADD COLUMN IF NOT EXISTS exige_validacao_telefone_snapshot BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_recuperacoes_senha_cliente_ecossistema_id
    ON recuperacoes_senha (cliente_ecossistema_id);
