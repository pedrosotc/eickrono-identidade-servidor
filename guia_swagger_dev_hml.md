# Guia de Acesso ao Swagger em Dev e HML

Este guia resume como abrir o Swagger/OpenAPI do `eickrono-identidade-servidor`
em `dev` e `hml`.

## 1. Dev local

### 1.1 Swagger UI

- `http://localhost:8081/swagger-ui/index.html`

### 1.2 OpenAPI JSON

- `http://localhost:8081/v3/api-docs`

### 1.3 Credenciais

- em `dev`, o Swagger fica liberado para uso local;
- nao exige `Basic Auth`.

## 2. HML local

### 2.1 Swagger UI

- `http://localhost:18081/swagger-ui/index.html`

### 2.2 OpenAPI JSON

- `http://localhost:18081/v3/api-docs`

### 2.3 Credenciais

- usuario: `swagger`
- senha: `Sw9@Qm2!Tx7#Lp4$Vz8Kr`

## 3. HML publico na AWS

### 3.1 Swagger UI

- `https://id-hml.eickrono.store/swagger-ui/index.html`

### 3.2 OpenAPI JSON

- `https://id-hml.eickrono.store/v3/api-docs`

### 3.3 Credenciais

- usuario: `swagger`
- senha: `Sw9@Qm2!Tx7#Lp4$Vz8Kr`

### 3.4 Comportamento atual esperado

No `hml` publico, o Swagger esta:

- habilitado;
- protegido por `Basic Auth`;
- protegido por whitelist de IP.

Entao, na pratica:

- se o acesso vier de IP permitido e com credencial correta, o Swagger abre;
- se o acesso vier de IP fora da whitelist, o retorno esperado e `403`.

## 4. Resumo rapido

| Ambiente | Swagger UI | OpenAPI JSON | Credencial |
| --- | --- | --- | --- |
| `dev` local | `http://localhost:8081/swagger-ui/index.html` | `http://localhost:8081/v3/api-docs` | nao exige |
| `hml` local | `http://localhost:18081/swagger-ui/index.html` | `http://localhost:18081/v3/api-docs` | `swagger / Sw9@Qm2!Tx7#Lp4$Vz8Kr` |
| `hml` publico | `https://id-hml.eickrono.store/swagger-ui/index.html` | `https://id-hml.eickrono.store/v3/api-docs` | `swagger / Sw9@Qm2!Tx7#Lp4$Vz8Kr` + IP permitido |
