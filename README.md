# Eickrono Identidade Servidor

Projeto canônico e independente da borda pública de identidade e autenticação da
Eickrono.

O nome do repositório agora é `eickrono-identidade-servidor`, mas o
identificador lógico do serviço em runtime continua sendo
`api-identidade-eickrono` por compatibilidade com o ecossistema atual
(Keycloak, audiences e compose local).

## Papel

- expor os endpoints publicos de cadastro, login, recuperacao de senha e sessao
- validar codigos e credenciais
- emitir e renovar sessao
- emitir `X-Device-Token`
- integrar com Keycloak e com os demais servicos do ecossistema por
  backchannel

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

## Docker

O `Dockerfile` deste projeto espera que o `jar` ja tenha sido empacotado em
`target/`.

## Relação com os demais serviços

- `eickrono-autenticacao-servidor`: servidor de autenticação/autorização baseado em Keycloak
- `eickrono-contas-servidor`: API de contas
