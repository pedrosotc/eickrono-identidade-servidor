WITH cliente_thimisu AS (
    SELECT id, exige_validacao_telefone
    FROM catalogo.clientes_ecossistema
    WHERE codigo = 'eickrono-thimisu-app'
)
UPDATE cadastros_conta AS cadastro
SET cliente_ecossistema_id = COALESCE(cadastro.cliente_ecossistema_id, cliente.id),
    exige_validacao_telefone_snapshot = cliente.exige_validacao_telefone
FROM cliente_thimisu cliente
WHERE cadastro.cliente_ecossistema_id IS NULL
   OR cadastro.exige_validacao_telefone_snapshot IS DISTINCT FROM cliente.exige_validacao_telefone;

WITH cliente_thimisu AS (
    SELECT id, exige_validacao_telefone
    FROM catalogo.clientes_ecossistema
    WHERE codigo = 'eickrono-thimisu-app'
)
UPDATE recuperacoes_senha AS recuperacao
SET cliente_ecossistema_id = COALESCE(recuperacao.cliente_ecossistema_id, cliente.id),
    exige_validacao_telefone_snapshot = cliente.exige_validacao_telefone
FROM cliente_thimisu cliente
WHERE recuperacao.cliente_ecossistema_id IS NULL
   OR recuperacao.exige_validacao_telefone_snapshot IS DISTINCT FROM cliente.exige_validacao_telefone;

UPDATE autenticacao.cadastros_conta cadastro
SET exige_validacao_telefone_snapshot = cliente.exige_validacao_telefone
FROM catalogo.clientes_ecossistema cliente
WHERE cliente.id = cadastro.cliente_ecossistema_id;

WITH cliente_thimisu AS (
    SELECT id
    FROM catalogo.clientes_ecossistema
    WHERE codigo = 'eickrono-thimisu-app'
)
UPDATE autenticacao.recuperacoes_senha AS recuperacao
SET cliente_ecossistema_id = COALESCE(recuperacao.cliente_ecossistema_id, cliente.id)
FROM cliente_thimisu cliente
WHERE recuperacao.cliente_ecossistema_id IS NULL;

UPDATE autenticacao.recuperacoes_senha recuperacao
SET exige_validacao_telefone_snapshot = cliente.exige_validacao_telefone
FROM catalogo.clientes_ecossistema cliente
WHERE cliente.id = recuperacao.cliente_ecossistema_id;
