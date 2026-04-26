ALTER TABLE cadastros_conta
    ADD COLUMN IF NOT EXISTS codigo_telefone_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS codigo_telefone_gerado_em TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS codigo_telefone_expira_em TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS telefone_confirmado_em TIMESTAMP WITH TIME ZONE;
