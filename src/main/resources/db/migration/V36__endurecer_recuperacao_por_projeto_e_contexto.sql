ALTER TABLE autenticacao.recuperacoes_senha
    ALTER COLUMN cliente_ecossistema_id SET NOT NULL;

ALTER TABLE recuperacoes_senha
    ALTER COLUMN cliente_ecossistema_id SET NOT NULL;
