CREATE SCHEMA IF NOT EXISTS identidade;

CREATE TABLE IF NOT EXISTS identidade.pessoas (
    id UUID PRIMARY KEY,
    nome_completo VARCHAR(255) NOT NULL,
    nome_exibicao VARCHAR(255),
    tipo_pessoa VARCHAR(16) NOT NULL DEFAULT 'FISICA',
    nome_fantasia VARCHAR(255),
    sexo VARCHAR(16),
    pais_nascimento VARCHAR(8),
    data_nascimento DATE,
    status_identidade VARCHAR(32) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS identidade.contatos_email (
    id UUID PRIMARY KEY,
    pessoa_id UUID NOT NULL REFERENCES identidade.pessoas (id) ON DELETE CASCADE,
    email_normalizado VARCHAR(255) NOT NULL UNIQUE,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    verificado_em TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_contatos_email_pessoa_principal
    ON identidade.contatos_email (pessoa_id, principal);

CREATE TABLE IF NOT EXISTS identidade.contatos_telefone (
    id UUID PRIMARY KEY,
    pessoa_id UUID NOT NULL REFERENCES identidade.pessoas (id) ON DELETE CASCADE,
    pais_iso VARCHAR(2),
    ddi VARCHAR(4) NOT NULL,
    ddd VARCHAR(8),
    numero_nacional VARCHAR(32) NOT NULL,
    telefone_normalizado VARCHAR(32) NOT NULL UNIQUE,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    verificado_em TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_contatos_telefone_pessoa_principal
    ON identidade.contatos_telefone (pessoa_id, principal);

CREATE INDEX IF NOT EXISTS idx_contatos_telefone_partes
    ON identidade.contatos_telefone (ddi, ddd, numero_nacional);

CREATE TABLE IF NOT EXISTS identidade.avatar_origens (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(64) NOT NULL UNIQUE,
    nome VARCHAR(128) NOT NULL,
    tipo VARCHAR(32) NOT NULL,
    cliente_ecossistema_id BIGINT REFERENCES catalogo.clientes_ecossistema (id),
    permite_vinculo_social BOOLEAN NOT NULL DEFAULT FALSE,
    permite_upload_usuario BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS identidade.avatar_usuario (
    id UUID PRIMARY KEY,
    usuario_cliente_id UUID NOT NULL REFERENCES autenticacao.usuarios_clientes_ecossistema (id) ON DELETE CASCADE,
    origem_id BIGINT NOT NULL REFERENCES identidade.avatar_origens (id),
    forma_acesso_id UUID REFERENCES autenticacao.usuarios_formas_acesso (id),
    nome_exibicao_externo VARCHAR(255),
    url_avatar VARCHAR(2048) NOT NULL,
    storage_key VARCHAR(1024),
    content_type VARCHAR(128),
    tamanho_bytes BIGINT,
    hash_conteudo VARCHAR(128),
    versao VARCHAR(128),
    preferido BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    removido_em TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_avatar_usuario_preferido_ativo
    ON identidade.avatar_usuario (usuario_cliente_id)
    WHERE preferido IS TRUE
      AND removido_em IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_avatar_usuario_forma_origem
    ON identidade.avatar_usuario (usuario_cliente_id, origem_id, forma_acesso_id)
    WHERE forma_acesso_id IS NOT NULL
      AND removido_em IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_avatar_usuario_storage_origem
    ON identidade.avatar_usuario (usuario_cliente_id, origem_id, storage_key)
    WHERE storage_key IS NOT NULL
      AND removido_em IS NULL;

CREATE TABLE IF NOT EXISTS autenticacao.cadastros_conta_avatares (
    id UUID PRIMARY KEY,
    cadastro_id UUID NOT NULL REFERENCES autenticacao.cadastros_conta (id) ON DELETE CASCADE,
    origem_id BIGINT NOT NULL REFERENCES identidade.avatar_origens (id),
    url_avatar VARCHAR(2048) NOT NULL,
    storage_key VARCHAR(1024),
    content_type VARCHAR(128),
    tamanho_bytes BIGINT,
    hash_conteudo VARCHAR(128),
    versao VARCHAR(128),
    preferido BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_cadastros_conta_avatares_preferido
    ON autenticacao.cadastros_conta_avatares (cadastro_id)
    WHERE preferido IS TRUE;

WITH cliente_thimisu AS (
    SELECT id
    FROM catalogo.clientes_ecossistema
    WHERE codigo = 'eickrono-thimisu-app'
    LIMIT 1
)
INSERT INTO identidade.avatar_origens (
    codigo,
    nome,
    tipo,
    cliente_ecossistema_id,
    permite_vinculo_social,
    permite_upload_usuario,
    ativo,
    criado_em,
    atualizado_em
)
VALUES
    ('GOOGLE', 'Google', 'PROVEDOR_SOCIAL', NULL, TRUE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('APPLE', 'Apple', 'PROVEDOR_SOCIAL', NULL, TRUE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FACEBOOK', 'Facebook', 'PROVEDOR_SOCIAL', NULL, TRUE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('LINKEDIN', 'LinkedIn', 'PROVEDOR_SOCIAL', NULL, TRUE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('INSTAGRAM', 'Instagram', 'PROVEDOR_SOCIAL', NULL, TRUE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('X', 'X', 'PROVEDOR_SOCIAL', NULL, TRUE, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (
        'THIMISU',
        'Thimisu',
        'PRODUTO_EICKRONO',
        (SELECT id FROM cliente_thimisu),
        FALSE,
        TRUE,
        TRUE,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (codigo) DO UPDATE
SET nome = EXCLUDED.nome,
    tipo = EXCLUDED.tipo,
    cliente_ecossistema_id = EXCLUDED.cliente_ecossistema_id,
    permite_vinculo_social = EXCLUDED.permite_vinculo_social,
    permite_upload_usuario = EXCLUDED.permite_upload_usuario,
    ativo = EXCLUDED.ativo,
    atualizado_em = CURRENT_TIMESTAMP;

WITH fontes AS (
    SELECT (
               SUBSTRING(MD5('identidade.pessoa:' || pessoa.sub) FROM 1 FOR 8) || '-' ||
               SUBSTRING(MD5('identidade.pessoa:' || pessoa.sub) FROM 9 FOR 4) || '-' ||
               SUBSTRING(MD5('identidade.pessoa:' || pessoa.sub) FROM 13 FOR 4) || '-' ||
               SUBSTRING(MD5('identidade.pessoa:' || pessoa.sub) FROM 17 FOR 4) || '-' ||
               SUBSTRING(MD5('identidade.pessoa:' || pessoa.sub) FROM 21 FOR 12)
           )::UUID AS id,
           pessoa.nome AS nome_completo,
           pessoa.nome AS nome_exibicao,
           'FISICA' AS tipo_pessoa,
           'ATIVO' AS status_identidade,
           COALESCE(pessoa.atualizado_em, CURRENT_TIMESTAMP) AS criado_em,
           COALESCE(pessoa.atualizado_em, CURRENT_TIMESTAMP) AS atualizado_em,
           1 AS prioridade
    FROM pessoas_identidade pessoa
    WHERE pessoa.sub IS NOT NULL
      AND BTRIM(pessoa.sub) <> ''
      AND pessoa.nome IS NOT NULL
      AND BTRIM(pessoa.nome) <> ''

    UNION ALL

    SELECT cadastro_novo.pessoa_id AS id,
           COALESCE(NULLIF(BTRIM(cadastro_legacy.nome_completo), ''), cadastro_novo.id::TEXT) AS nome_completo,
           NULLIF(BTRIM(cadastro_legacy.nome_completo), '') AS nome_exibicao,
           'FISICA' AS tipo_pessoa,
           CASE
               WHEN cadastro_novo.concluido_em IS NULL THEN 'PENDENTE'
               ELSE 'ATIVO'
           END AS status_identidade,
           COALESCE(cadastro_novo.criado_em, CURRENT_TIMESTAMP) AS criado_em,
           COALESCE(cadastro_novo.atualizado_em, CURRENT_TIMESTAMP) AS atualizado_em,
           2 AS prioridade
    FROM autenticacao.cadastros_conta cadastro_novo
    LEFT JOIN cadastros_conta cadastro_legacy
      ON cadastro_legacy.cadastro_id = cadastro_novo.id
    WHERE cadastro_novo.pessoa_id IS NOT NULL

    UNION ALL

    SELECT usuario.pessoa_id AS id,
           COALESCE(NULLIF(BTRIM(usuario.sub_remoto), ''), usuario.id::TEXT) AS nome_completo,
           NULLIF(BTRIM(usuario.sub_remoto), '') AS nome_exibicao,
           'FISICA' AS tipo_pessoa,
           usuario.status_global AS status_identidade,
           COALESCE(usuario.criado_em, CURRENT_TIMESTAMP) AS criado_em,
           COALESCE(usuario.atualizado_em, CURRENT_TIMESTAMP) AS atualizado_em,
           3 AS prioridade
    FROM autenticacao.usuarios usuario
    WHERE usuario.pessoa_id IS NOT NULL
),
fontes_deduplicadas AS (
    SELECT DISTINCT ON (id)
           id,
           nome_completo,
           nome_exibicao,
           tipo_pessoa,
           status_identidade,
           criado_em,
           atualizado_em
    FROM fontes
    WHERE id IS NOT NULL
      AND nome_completo IS NOT NULL
      AND BTRIM(nome_completo) <> ''
    ORDER BY id, prioridade
)
INSERT INTO identidade.pessoas (
    id,
    nome_completo,
    nome_exibicao,
    tipo_pessoa,
    status_identidade,
    criado_em,
    atualizado_em
)
SELECT id,
       nome_completo,
       nome_exibicao,
       tipo_pessoa,
       status_identidade,
       criado_em,
       atualizado_em
FROM fontes_deduplicadas
ON CONFLICT (id) DO UPDATE
SET nome_completo = EXCLUDED.nome_completo,
    nome_exibicao = COALESCE(EXCLUDED.nome_exibicao, identidade.pessoas.nome_exibicao),
    tipo_pessoa = EXCLUDED.tipo_pessoa,
    status_identidade = EXCLUDED.status_identidade,
    atualizado_em = EXCLUDED.atualizado_em;

WITH emails AS (
    SELECT (
               SUBSTRING(MD5('identidade.email:' || LOWER(BTRIM(pessoa.email))) FROM 1 FOR 8) || '-' ||
               SUBSTRING(MD5('identidade.email:' || LOWER(BTRIM(pessoa.email))) FROM 9 FOR 4) || '-' ||
               SUBSTRING(MD5('identidade.email:' || LOWER(BTRIM(pessoa.email))) FROM 13 FOR 4) || '-' ||
               SUBSTRING(MD5('identidade.email:' || LOWER(BTRIM(pessoa.email))) FROM 17 FOR 4) || '-' ||
               SUBSTRING(MD5('identidade.email:' || LOWER(BTRIM(pessoa.email))) FROM 21 FOR 12)
           )::UUID AS id,
           (
               SUBSTRING(MD5('identidade.pessoa:' || pessoa.sub) FROM 1 FOR 8) || '-' ||
               SUBSTRING(MD5('identidade.pessoa:' || pessoa.sub) FROM 9 FOR 4) || '-' ||
               SUBSTRING(MD5('identidade.pessoa:' || pessoa.sub) FROM 13 FOR 4) || '-' ||
               SUBSTRING(MD5('identidade.pessoa:' || pessoa.sub) FROM 17 FOR 4) || '-' ||
               SUBSTRING(MD5('identidade.pessoa:' || pessoa.sub) FROM 21 FOR 12)
           )::UUID AS pessoa_id,
           LOWER(BTRIM(pessoa.email)) AS email_normalizado,
           TRUE AS principal,
           pessoa.atualizado_em AS verificado_em,
           COALESCE(pessoa.atualizado_em, CURRENT_TIMESTAMP) AS criado_em,
           COALESCE(pessoa.atualizado_em, CURRENT_TIMESTAMP) AS atualizado_em,
           1 AS prioridade
    FROM pessoas_identidade pessoa
    WHERE pessoa.sub IS NOT NULL
      AND BTRIM(pessoa.sub) <> ''
      AND pessoa.email IS NOT NULL
      AND BTRIM(pessoa.email) <> ''

    UNION ALL

    SELECT cadastro_novo.email_id AS id,
           cadastro_novo.pessoa_id,
           LOWER(BTRIM(cadastro_legacy.email_principal)) AS email_normalizado,
           TRUE AS principal,
           cadastro_novo.email_confirmado_em AS verificado_em,
           COALESCE(cadastro_novo.criado_em, CURRENT_TIMESTAMP) AS criado_em,
           COALESCE(cadastro_novo.atualizado_em, CURRENT_TIMESTAMP) AS atualizado_em,
           2 AS prioridade
    FROM autenticacao.cadastros_conta cadastro_novo
    JOIN cadastros_conta cadastro_legacy
      ON cadastro_legacy.cadastro_id = cadastro_novo.id
    WHERE cadastro_novo.email_id IS NOT NULL
      AND cadastro_novo.pessoa_id IS NOT NULL
      AND cadastro_legacy.email_principal IS NOT NULL
      AND BTRIM(cadastro_legacy.email_principal) <> ''
),
emails_deduplicados AS (
    SELECT DISTINCT ON (email_normalizado)
           id,
           pessoa_id,
           email_normalizado,
           principal,
           verificado_em,
           criado_em,
           atualizado_em
    FROM emails
    WHERE email_normalizado IS NOT NULL
      AND email_normalizado <> ''
    ORDER BY email_normalizado, prioridade
)
INSERT INTO identidade.contatos_email (
    id,
    pessoa_id,
    email_normalizado,
    principal,
    verificado_em,
    criado_em,
    atualizado_em
)
SELECT email.id,
       email.pessoa_id,
       email.email_normalizado,
       email.principal,
       email.verificado_em,
       email.criado_em,
       email.atualizado_em
FROM emails_deduplicados email
JOIN identidade.pessoas pessoa
  ON pessoa.id = email.pessoa_id
ON CONFLICT (email_normalizado) DO UPDATE
SET pessoa_id = EXCLUDED.pessoa_id,
    principal = EXCLUDED.principal,
    verificado_em = COALESCE(EXCLUDED.verificado_em, identidade.contatos_email.verificado_em),
    atualizado_em = EXCLUDED.atualizado_em;

WITH telefones_brutos AS (
    SELECT cadastro_novo.telefone_id AS id,
           cadastro_novo.pessoa_id,
           cadastro_legacy.telefone_principal,
           cadastro_legacy.telefone_confirmado_em AS verificado_em,
           COALESCE(cadastro_novo.criado_em, CURRENT_TIMESTAMP) AS criado_em,
           COALESCE(cadastro_novo.atualizado_em, CURRENT_TIMESTAMP) AS atualizado_em
    FROM autenticacao.cadastros_conta cadastro_novo
    JOIN cadastros_conta cadastro_legacy
      ON cadastro_legacy.cadastro_id = cadastro_novo.id
    WHERE cadastro_novo.telefone_id IS NOT NULL
      AND cadastro_novo.pessoa_id IS NOT NULL
      AND cadastro_legacy.telefone_principal IS NOT NULL
      AND BTRIM(cadastro_legacy.telefone_principal) <> ''
),
telefones_normalizados AS (
    SELECT id,
           pessoa_id,
           REGEXP_REPLACE(telefone_principal, '[^0-9]', '', 'g') AS digitos,
           verificado_em,
           criado_em,
           atualizado_em
    FROM telefones_brutos
),
telefones_deduplicados AS (
    SELECT DISTINCT ON (digitos)
           id,
           pessoa_id,
           digitos,
           verificado_em,
           criado_em,
           atualizado_em
    FROM telefones_normalizados
    WHERE digitos IS NOT NULL
      AND digitos <> ''
    ORDER BY digitos, atualizado_em DESC
)
INSERT INTO identidade.contatos_telefone (
    id,
    pessoa_id,
    pais_iso,
    ddi,
    ddd,
    numero_nacional,
    telefone_normalizado,
    principal,
    verificado_em,
    criado_em,
    atualizado_em
)
SELECT telefone.id,
       telefone.pessoa_id,
       CASE WHEN telefone.digitos LIKE '55%' THEN 'BR' ELSE NULL END,
       CASE
           WHEN telefone.digitos LIKE '55%' THEN '55'
           ELSE SUBSTRING(telefone.digitos FROM 1 FOR LEAST(4, LENGTH(telefone.digitos)))
       END,
       CASE
           WHEN telefone.digitos LIKE '55%' AND LENGTH(telefone.digitos) > 4
               THEN SUBSTRING(telefone.digitos FROM 3 FOR 2)
           ELSE NULL
       END,
       CASE
           WHEN telefone.digitos LIKE '55%' AND LENGTH(telefone.digitos) > 4
               THEN SUBSTRING(telefone.digitos FROM 5)
           ELSE telefone.digitos
       END,
       '+' || telefone.digitos,
       TRUE,
       telefone.verificado_em,
       telefone.criado_em,
       telefone.atualizado_em
FROM telefones_deduplicados telefone
JOIN identidade.pessoas pessoa
  ON pessoa.id = telefone.pessoa_id
ON CONFLICT (telefone_normalizado) DO UPDATE
SET pessoa_id = EXCLUDED.pessoa_id,
    principal = EXCLUDED.principal,
    verificado_em = COALESCE(EXCLUDED.verificado_em, identidade.contatos_telefone.verificado_em),
    atualizado_em = EXCLUDED.atualizado_em;

WITH avatares_sociais AS (
    SELECT (
               SUBSTRING(MD5(
                   'identidade.avatar_usuario:' || uce.id::TEXT || ':' ||
                   origem.id::TEXT || ':' || ufa.id::TEXT
               ) FROM 1 FOR 8) || '-' ||
               SUBSTRING(MD5(
                   'identidade.avatar_usuario:' || uce.id::TEXT || ':' ||
                   origem.id::TEXT || ':' || ufa.id::TEXT
               ) FROM 9 FOR 4) || '-' ||
               SUBSTRING(MD5(
                   'identidade.avatar_usuario:' || uce.id::TEXT || ':' ||
                   origem.id::TEXT || ':' || ufa.id::TEXT
               ) FROM 13 FOR 4) || '-' ||
               SUBSTRING(MD5(
                   'identidade.avatar_usuario:' || uce.id::TEXT || ':' ||
                   origem.id::TEXT || ':' || ufa.id::TEXT
               ) FROM 17 FOR 4) || '-' ||
               SUBSTRING(MD5(
                   'identidade.avatar_usuario:' || uce.id::TEXT || ':' ||
                   origem.id::TEXT || ':' || ufa.id::TEXT
               ) FROM 21 FOR 12)
           )::UUID AS id,
           uce.id AS usuario_cliente_id,
           origem.id AS origem_id,
           ufa.id AS forma_acesso_id,
           ufa.nome_exibicao_externo,
           ufa.url_avatar_externo AS url_avatar,
           NULL::VARCHAR AS storage_key,
           NULL::VARCHAR AS content_type,
           NULL::BIGINT AS tamanho_bytes,
           NULL::VARCHAR AS hash_conteudo,
           COALESCE(ufa.avatar_externo_atualizado_em, ufa.vinculado_em, uce.atualizado_em)::TEXT AS versao,
           (uce.avatar_preferido_forma_acesso_id = ufa.id) AS preferido,
           COALESCE(ufa.vinculado_em, uce.vinculado_em, CURRENT_TIMESTAMP) AS criado_em,
           COALESCE(ufa.avatar_externo_atualizado_em, uce.atualizado_em, CURRENT_TIMESTAMP) AS atualizado_em
    FROM autenticacao.usuarios_formas_acesso ufa
    JOIN autenticacao.usuarios_clientes_ecossistema uce
      ON uce.usuario_id = ufa.usuario_id
     AND uce.revogado_em IS NULL
    JOIN identidade.avatar_origens origem
      ON origem.codigo = UPPER(BTRIM(ufa.provedor))
    WHERE ufa.tipo = 'SOCIAL'
      AND ufa.desvinculado_em IS NULL
      AND ufa.url_avatar_externo IS NOT NULL
      AND BTRIM(ufa.url_avatar_externo) <> ''
)
INSERT INTO identidade.avatar_usuario (
    id,
    usuario_cliente_id,
    origem_id,
    forma_acesso_id,
    nome_exibicao_externo,
    url_avatar,
    storage_key,
    content_type,
    tamanho_bytes,
    hash_conteudo,
    versao,
    preferido,
    criado_em,
    atualizado_em
)
SELECT id,
       usuario_cliente_id,
       origem_id,
       forma_acesso_id,
       nome_exibicao_externo,
       url_avatar,
       storage_key,
       content_type,
       tamanho_bytes,
       hash_conteudo,
       versao,
       preferido,
       criado_em,
       atualizado_em
FROM avatares_sociais
ON CONFLICT (id) DO UPDATE
SET nome_exibicao_externo = COALESCE(EXCLUDED.nome_exibicao_externo, identidade.avatar_usuario.nome_exibicao_externo),
    url_avatar = EXCLUDED.url_avatar,
    versao = EXCLUDED.versao,
    preferido = EXCLUDED.preferido,
    atualizado_em = EXCLUDED.atualizado_em,
    removido_em = NULL;

WITH avatar_legado_preferido AS (
    SELECT (
               SUBSTRING(MD5(
                   'identidade.avatar_usuario:legado:' || uce.id::TEXT || ':' ||
                   origem.id::TEXT || ':' || uce.avatar_preferido_url
               ) FROM 1 FOR 8) || '-' ||
               SUBSTRING(MD5(
                   'identidade.avatar_usuario:legado:' || uce.id::TEXT || ':' ||
                   origem.id::TEXT || ':' || uce.avatar_preferido_url
               ) FROM 9 FOR 4) || '-' ||
               SUBSTRING(MD5(
                   'identidade.avatar_usuario:legado:' || uce.id::TEXT || ':' ||
                   origem.id::TEXT || ':' || uce.avatar_preferido_url
               ) FROM 13 FOR 4) || '-' ||
               SUBSTRING(MD5(
                   'identidade.avatar_usuario:legado:' || uce.id::TEXT || ':' ||
                   origem.id::TEXT || ':' || uce.avatar_preferido_url
               ) FROM 17 FOR 4) || '-' ||
               SUBSTRING(MD5(
                   'identidade.avatar_usuario:legado:' || uce.id::TEXT || ':' ||
                   origem.id::TEXT || ':' || uce.avatar_preferido_url
               ) FROM 21 FOR 12)
           )::UUID AS id,
           uce.id AS usuario_cliente_id,
           origem.id AS origem_id,
           uce.avatar_preferido_url AS url_avatar,
           COALESCE(uce.avatar_preferido_atualizado_em, uce.atualizado_em, CURRENT_TIMESTAMP)::TEXT AS versao,
           COALESCE(uce.avatar_preferido_atualizado_em, uce.atualizado_em, CURRENT_TIMESTAMP) AS atualizado_em
    FROM autenticacao.usuarios_clientes_ecossistema uce
    JOIN identidade.avatar_origens origem
      ON origem.codigo = CASE
          WHEN uce.avatar_preferido_origem = 'SOCIAL' THEN 'THIMISU'
          WHEN uce.avatar_preferido_origem IN ('UPLOAD_USUARIO', 'URL_EXTERNA') THEN 'THIMISU'
          ELSE 'THIMISU'
      END
    WHERE uce.avatar_preferido_url IS NOT NULL
      AND BTRIM(uce.avatar_preferido_url) <> ''
      AND NOT EXISTS (
          SELECT 1
          FROM identidade.avatar_usuario atual
          WHERE atual.usuario_cliente_id = uce.id
            AND atual.preferido IS TRUE
            AND atual.removido_em IS NULL
      )
)
INSERT INTO identidade.avatar_usuario (
    id,
    usuario_cliente_id,
    origem_id,
    forma_acesso_id,
    nome_exibicao_externo,
    url_avatar,
    storage_key,
    content_type,
    tamanho_bytes,
    hash_conteudo,
    versao,
    preferido,
    criado_em,
    atualizado_em
)
SELECT id,
       usuario_cliente_id,
       origem_id,
       NULL,
       NULL,
       url_avatar,
       NULL,
       NULL,
       NULL,
       NULL,
       versao,
       TRUE,
       atualizado_em,
       atualizado_em
FROM avatar_legado_preferido
ON CONFLICT (id) DO UPDATE
SET url_avatar = EXCLUDED.url_avatar,
    versao = EXCLUDED.versao,
    preferido = EXCLUDED.preferido,
    atualizado_em = EXCLUDED.atualizado_em,
    removido_em = NULL;
