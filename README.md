# Eickrono Identidade Servidor

Projeto canônico e independente do serviço de identidade e contexto canônico da
Eickrono.

O nome do repositório agora é `eickrono-identidade-servidor`, mas o
identificador lógico do serviço em runtime continua sendo
`api-identidade-eickrono` por compatibilidade com o ecossistema atual
(Keycloak, audiences e compose local).

## Diretriz de nomenclatura

No contexto de identidade, modelos, contratos, tabelas, enums e documentação
devem evitar nomes específicos de produto quando o conceito for compartilhado
por vários sistemas do ecossistema.

Regra prática:

- preferir nomes gerais como `cliente`, `sistema`, `vinculo`, `perfilSistema`
  e equivalentes;
- evitar carregar nomes como `Thimisu` para conceitos que pertencem ao
  ecossistema inteiro;
- usar o nome de um produto só quando a regra for realmente exclusiva dele.

## Papel

### Alvo arquitetural aprovado

- concentrar a leitura e a escrita da `Pessoa` canônica;
- expor contexto de identidade compartilhado para os demais serviços internos;
- responder por vínculos, dados e contexto canônico que a autenticação ainda
  precise consultar por backchannel;
- deixar de ser a borda pública final do app para cadastro, login, refresh e
  recuperação de senha.

### Estado atual do runtime

- este serviço ainda carrega contratos públicos transitórios usados pelo app em
  parte do runtime atual;
- essa exposição pública atual deve ser lida como estado de migração, não como
  fronteira final aprovada.

## Build e teste

```bash
make test
```

A suíte completa usa `Testcontainers` em parte dos testes de integração. Se o
ambiente local não tiver Docker funcional para o Maven, rode primeiro os
unitários ou a suíte parcial representativa, por exemplo:

```bash
make test-rapido
```

Para empacotar:

```bash
make package
```

## Consulta de versão em runtime

O serviço expõe duas formas de consulta operacional:

- `GET /api/v1/estado`
- `GET /actuator/info`

O endpoint `/api/v1/estado` devolve:

- `servico`
- `status`
- `versao`
- `buildTime`

## Docker

O `Dockerfile` deste projeto espera que o `jar` ja tenha sido empacotado em
`target/`.

## Relação com os demais serviços

- `eickrono-autenticacao-servidor`: servidor central de autenticação/autorização
  baseado em Keycloak e futura borda pública final do app
- `eickrono-contas-servidor`: domínio separado de contas, fora da fronteira
  interna da autenticação

## Documentação operacional

- [Checklist Técnico: Vínculos Sociais Plurais no Cadastro](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/checklist_tecnico_vinculos_sociais_plural_cadastro.md)
