ALTER TABLE cadastros_conta
    ADD COLUMN IF NOT EXISTS convite_organizacional_codigo VARCHAR(128),
    ADD COLUMN IF NOT EXISTS convite_organizacional_organizacao_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS convite_organizacional_nome_organizacao VARCHAR(255),
    ADD COLUMN IF NOT EXISTS convite_organizacional_email_convidado VARCHAR(255),
    ADD COLUMN IF NOT EXISTS convite_organizacional_exige_conta_separada BOOLEAN;
