CREATE TABLE IF NOT EXISTS identidade.pendencias_remocao_avatar_usuario (
    id UUID PRIMARY KEY,
    correlacao_id UUID NOT NULL,
    avatar_id UUID NOT NULL REFERENCES identidade.avatar_usuario (id),
    usuario_cliente_id UUID NOT NULL,
    produto VARCHAR(64) NOT NULL,
    origem VARCHAR(64) NOT NULL,
    bucket VARCHAR(255) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL,
    tentativas INTEGER NOT NULL DEFAULT 0,
    erro_codigo VARCHAR(128),
    erro_mensagem TEXT,
    solicitada_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processada_em TIMESTAMP WITH TIME ZONE,
    retencao_expira_em TIMESTAMP WITH TIME ZONE,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pendencias_remocao_avatar_usuario_avatar UNIQUE (avatar_id, storage_key)
);

COMMENT ON TABLE identidade.pendencias_remocao_avatar_usuario IS
    'Pendencias tecnicas para remocao fisica de avatares controlados pela Eickrono antes da limpeza logica do registro.';
COMMENT ON COLUMN identidade.pendencias_remocao_avatar_usuario.correlacao_id IS
    'Correlacao da exclusao de cadastro/produto aberta pelo orquestrador administrativo.';
COMMENT ON COLUMN identidade.pendencias_remocao_avatar_usuario.avatar_id IS
    'Avatar logico que originou a pendencia de remocao fisica.';
COMMENT ON COLUMN identidade.pendencias_remocao_avatar_usuario.usuario_cliente_id IS
    'Vinculo usuario/produto dono do avatar no momento da materializacao da pendencia.';
COMMENT ON COLUMN identidade.pendencias_remocao_avatar_usuario.produto IS
    'Produto Eickrono associado ao avatar, por exemplo THIMISU.';
COMMENT ON COLUMN identidade.pendencias_remocao_avatar_usuario.origem IS
    'Origem do avatar materializada no momento da pendencia, como THIMISU, GOOGLE ou APPLE.';
COMMENT ON COLUMN identidade.pendencias_remocao_avatar_usuario.bucket IS
    'Bucket de storage onde o objeto fisico deve ser removido.';
COMMENT ON COLUMN identidade.pendencias_remocao_avatar_usuario.storage_key IS
    'Chave do objeto fisico no storage; a remocao nunca deve depender de URL publica.';
COMMENT ON COLUMN identidade.pendencias_remocao_avatar_usuario.status IS
    'Estado da remocao fisica: PENDENTE, PROCESSANDO, REMOVIDA, FALHOU ou IGNORADA.';
COMMENT ON COLUMN identidade.pendencias_remocao_avatar_usuario.tentativas IS
    'Quantidade de tentativas do worker/Lambda para apagar o objeto fisico.';
COMMENT ON COLUMN identidade.pendencias_remocao_avatar_usuario.retencao_expira_em IS
    'Data em que a pendencia tecnica pode ser removida da base depois da conclusao ou falha tratada.';

CREATE INDEX IF NOT EXISTS idx_pendencias_remocao_avatar_usuario_correlacao
    ON identidade.pendencias_remocao_avatar_usuario (correlacao_id);

CREATE INDEX IF NOT EXISTS idx_pendencias_remocao_avatar_usuario_status
    ON identidade.pendencias_remocao_avatar_usuario (status, solicitada_em);

CREATE INDEX IF NOT EXISTS idx_pendencias_remocao_avatar_usuario_usuario_cliente
    ON identidade.pendencias_remocao_avatar_usuario (usuario_cliente_id);
