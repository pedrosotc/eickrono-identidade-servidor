UPDATE catalogo.clientes_ecossistema
SET tipo_produto_exibicao = 'app',
    produto_exibicao = 'Thimisu',
    canal_exibicao = 'mobile',
    exige_validacao_telefone = FALSE,
    atualizado_em = CURRENT_TIMESTAMP
WHERE codigo = 'eickrono-thimisu-app';
