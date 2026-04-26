CREATE TABLE IF NOT EXISTS convites_organizacionais (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(128) NOT NULL UNIQUE,
    organizacao_id VARCHAR(128) NOT NULL,
    nome_organizacao VARCHAR(255) NOT NULL,
    email_convidado VARCHAR(255),
    nome_convidado VARCHAR(255),
    exige_conta_separada BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(16) NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_convites_organizacionais_codigo
    ON convites_organizacionais (codigo);
