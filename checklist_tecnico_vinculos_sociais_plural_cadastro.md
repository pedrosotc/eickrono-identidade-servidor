# Checklist Técnico: Vínculos Sociais Plurais no Cadastro

## Premissa canônica

Esta correção deve preservar três conceitos diferentes:

- `vínculo social persistido`: cada rede social autenticada e não removida deve
  permanecer vinculada à pessoa;
- `principal` em `formas_acesso`: prioridade técnica/canônica da forma de
  acesso, não escolha visual de avatar;
- `avatar preferido`: preferência visual por projeto, persistida em
  `autenticacao.usuarios_clientes_ecossistema.avatar_preferido_*`.

Regras duras:

- `Google`, `Apple` e quaisquer outras redes autenticadas no cadastro não podem
  competir entre si por um único slot singular de persistência;
- o campo singular legado `vinculoSocialPendente` não pode decidir qual rede
  “sobrevive” no backend;
- a escolha do avatar principal não define a “rede principal”;
- quando o usuário escolher uma foto processada localmente no dispositivo como
  foto pública do perfil, essa imagem precisa subir ao servidor e virar uma URL
  pública estável consumível pelos demais usuários do app.

## Estado validado hoje

### O booleano `principal` continua existindo

Sim. O booleano `principal` ainda existe no modelo atual de formas de acesso:

- [FormaAcesso.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/dominio/modelo/FormaAcesso.java)
- [V3__introduzir_pessoas_e_formas_acesso.sql](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/resources/db/migration/V3__introduzir_pessoas_e_formas_acesso.sql)
- [V15__criar_catalogo_e_usuarios_multiapp.sql](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/resources/db/migration/V15__criar_catalogo_e_usuarios_multiapp.sql)

Ele não foi substituído por `avatar_preferido_*`.

Leitura correta:

- `principal` em `formas_acesso` continua sendo metadado da forma de acesso;
- `avatar_preferido_*` continua sendo a preferência visual por projeto;
- as duas estruturas coexistem e não devem ser confundidas.

### O que já está correto

- a migration de avatar preferido já separa a preferência visual do vínculo
  social:
  - [V37__adicionar_avatar_social_e_avatar_preferido.sql](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/resources/db/migration/V37__adicionar_avatar_social_e_avatar_preferido.sql)
- o runtime autenticado já sabe:
  - listar vínculos sociais;
  - atualizar avatar preferido;
  - trabalhar com `url_avatar_externo`;
  - manter `avatar_preferido_*` por projeto.
- os serviços que devem ser preservados conceitualmente são:
  - [AvatarSocialProjetoJdbc.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/aplicacao/servico/AvatarSocialProjetoJdbc.java)
  - [VinculoSocialService.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/aplicacao/servico/VinculoSocialService.java)

### O que está errado

- a borda pública de cadastro ainda aceita e consome apenas
  `vinculoSocialPendente`:
  - [CadastroApiRequest.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/apresentacao/dto/fluxo/CadastroApiRequest.java)
  - [FluxoPublicoController.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/apresentacao/api/FluxoPublicoController.java)
- `CadastroConta` ainda modela vínculo pendente como singular:
  - [CadastroConta.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/dominio/modelo/CadastroConta.java)
- a finalização do cadastro ainda vincula apenas uma identidade federada:
  - [CadastroContaInternaServico.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/aplicacao/servico/CadastroContaInternaServico.java)

Consequência já observada em `hml`:

- o app autentica múltiplas redes durante o cadastro;
- o payload público já carrega plural;
- o `identidade-hml` persiste apenas uma;
- a outra rede cai depois em `federated_identity_account_exists` no login
  social.

## Checklist por arquivo e método

### 1. DTO público de cadastro

Arquivo:

- [CadastroApiRequest.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/apresentacao/dto/fluxo/CadastroApiRequest.java)

Checklist:

- adicionar `List<@Valid VinculoSocialPendenteApiRequest> vinculosSociaisPendentes`;
- manter `vinculoSocialPendente` apenas por compatibilidade;
- documentar no próprio DTO que o campo canônico para persistência plural é
  `vinculosSociaisPendentes`;
- não associar semântica de “principal” a esse campo singular legado.

### 2. DTO de vínculo social pendente

Arquivo:

- [VinculoSocialPendenteApiRequest.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/apresentacao/dto/fluxo/VinculoSocialPendenteApiRequest.java)

Checklist:

- alinhar o contrato ao runtime mais novo:
  - `provedor`
  - `identificadorExterno`
  - `contextoSocialPendenteId`
  - `nomeUsuarioExterno`
  - `email`
  - `nomeCompleto`
  - `urlAvatarExterno`
- manter `urlAvatarExterno` como metadado do vínculo/contexto, não como escolha
  de avatar preferido;
- permitir que Apple sem foto continue vinculada com `urlAvatarExterno = null`.

### 3. Controller público do cadastro

Arquivo:

- [FluxoPublicoController.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/apresentacao/api/FluxoPublicoController.java)

Método impactado:

- `criarCadastroPublico(...)`

Checklist:

- introduzir helper local para:
  - mesclar `vinculoSocialPendente` + `vinculosSociaisPendentes`;
  - deduplicar por `provedor + identificadorExterno`;
  - preservar ordem apenas para UX, não para decidir persistência;
- deixar explícito que a rede escolhida como avatar não define a única rede a
  ser persistida;
- persistir ou atualizar todos os contextos sociais pendentes do cadastro;
- parar de repassar só o singular diretamente para o serviço.

Referência útil do runtime mais novo:

- [FluxoPublicoController.java](/Users/thiago/Desenvolvedor/flutter/eickrono-autenticacao-servidor/modulos/modulo-eickrono-autenticacao/src/main/java/com/eickrono/api/identidade/apresentacao/api/FluxoPublicoController.java)

### 4. Persistência plural dos contextos sociais

Arquivo:

- [ContextoSocialPendenteJdbc.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/aplicacao/servico/ContextoSocialPendenteJdbc.java)

Checklist:

- usar `ContextoSocialPendenteJdbc` como fonte plural de verdade para o cadastro
  em andamento;
- garantir que cada rede autenticada e não removida tenha um contexto próprio;
- carregar `url_avatar_externo`, `nome_exibicao_externo` e `email` por contexto
  quando disponíveis;
- consumir o contexto apenas depois da vinculação federada realmente concluída.

### 5. Serviço de cadastro

Arquivo:

- [CadastroContaInternaServico.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/aplicacao/servico/CadastroContaInternaServico.java)

Métodos impactados:

- `cadastrarPublico(...)`
- `finalizarCadastroPublico(...)`

Checklist:

- em `cadastrarPublico(...)`, parar de assumir que o singular é suficiente como
  representação do vínculo social do cadastro;
- em `finalizarCadastroPublico(...)`, parar de usar
  `cadastroConta.possuiVinculoSocialPendente()` como única chave de decisão;
- carregar todos os contextos sociais pendentes compatíveis com o cadastro e o
  projeto;
- iterar todos os contextos e, para cada um:
  - chamar `clienteAdministracaoCadastroKeycloak.vincularIdentidadeFederada(...)`;
  - materializar o vínculo local, se necessário;
  - consumir aquele contexto;
- manter o singular em `CadastroConta` só como compatibilidade transitória, se
  ainda houver clientes legados.

### 6. Agregado `CadastroConta`

Arquivo:

- [CadastroConta.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/dominio/modelo/CadastroConta.java)

Checklist:

- não usar mais os campos:
  - `vinculoSocialPendenteProvedor`
  - `vinculoSocialPendenteIdentificadorExterno`
  - `vinculoSocialPendenteNomeUsuarioExterno`
  como fonte canônica de múltiplas redes;
- se esses campos permanecerem por compatibilidade, marcar sua semântica como
  transitória/legada;
- evitar nova expansão ad hoc do agregado com “segundo vínculo”, “terceiro
  vínculo” etc.

### 7. Cliente admin do Keycloak

Arquivo:

- [ClienteAdministracaoCadastroKeycloakHttp.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/infraestrutura/integracao/ClienteAdministracaoCadastroKeycloakHttp.java)

Método preservado:

- `vincularIdentidadeFederada(...)`

Checklist:

- não há mudança conceitual necessária aqui;
- este método já aceita o contrato necessário para vincular múltiplas redes;
- a mudança está em chamá-lo uma vez para cada contexto social pendente, e não
  apenas uma vez por cadastro.

### 8. Fluxo público de login social

Arquivo:

- [FluxoPublicoController.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/apresentacao/api/FluxoPublicoController.java)

Método impactado:

- endpoint `POST /api/publica/sessoes/sociais`

Checklist:

- diferenciar `autenticacao_social_invalida` de
  `federated_identity_account_exists`;
- tratar `federated_identity_account_exists` como erro técnico intermediário,
  não como resposta funcional final;
- classificar pelo menos estes subcasos:
  - `conta_local_existente_mesmo_email_sem_vinculo_social`:
    a conta do projeto já existe, a rede ainda não está ligada a ela e a ação
    correta é `ENTRAR_E_VINCULAR`;
  - `identidade_social_ja_vinculada_a_outro_usuario`:
    a rede social já pertence a outro usuário local e não pode ser vinculada
    automaticamente a esta conta;
  - `conflito_social_ambíguo`:
    existe colisão suficiente para impedir decisão automática e o backend deve
    bloquear a continuidade com erro funcional explícito;
- não colapsar esses casos em erro genérico de autenticação social.

Observação:

- este item é adjacente ao bug de cadastro plural;
- ele continua necessário mesmo depois da correção plural, porque o caso de
  conta existente sem vínculo continua sendo um fluxo legítimo do sistema.

### 8.1 Classificação funcional recomendada para colisão de broker

Regra canônica:

- uma identidade social específica pode estar ligada a um único usuário local;
- um usuário local pode ter várias redes sociais vinculadas;
- portanto, o mesmo erro técnico do Keycloak não pode produzir sempre a mesma
  resposta de negócio.

Classificação recomendada:

| Sinal técnico | Leitura funcional | Próximo passo |
| --- | --- | --- |
| `federated_identity_account_exists` + conta local do projeto já existe para o mesmo e-mail + a identidade social ainda não está ligada a essa conta | `conta_local_existente_mesmo_email_sem_vinculo_social` | oferecer `ENTRAR_E_VINCULAR` |
| `federated_identity_account_exists` + a identidade social já está ligada a outro usuário local | `identidade_social_ja_vinculada_a_outro_usuario` | erro explícito; não oferecer vinculação automática |
| `federated_identity_account_exists` + o backend não consegue provar qual conta é a correta | `conflito_social_ambíguo` | erro explícito ou suporte; não abrir cadastro nem vinculação automática |

Resposta funcional esperada:

- `ENTRAR_E_VINCULAR` só quando o backend puder provar a conta local correta;
- conflito duro quando a rede social já pertence a outro usuário;
- erro genérico de autenticação social apenas para falhas reais do fluxo de
  token exchange, e não para colisões classificáveis.

### 9. Avatar preferido e foto local pública

Arquivos a preservar semanticamente:

- [AvatarSocialProjetoJdbc.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/aplicacao/servico/AvatarSocialProjetoJdbc.java)
- [VinculoSocialService.java](/Users/thiago/Desenvolvedor/flutter/eickrono-identidade-servidor/src/main/java/com/eickrono/api/identidade/aplicacao/servico/VinculoSocialService.java)

Checklist:

- manter `avatar_preferido_*` como estado por projeto;
- manter `principal` das formas de acesso como atributo da forma de acesso, não
  como substituto do avatar preferido;
- quando o usuário escolher foto do dispositivo como avatar público, garantir
  pipeline de persistência remota:
  - upload/armazenamento;
  - URL pública estável;
  - gravação em `avatar_preferido_url` ou `avatar_preferido_arquivo_id`;
- não tratar essa persistência como opcional na regra canônica.

## Ordem recomendada

1. alinhar `CadastroApiRequest` e `VinculoSocialPendenteApiRequest`;
2. portar o merge plural para `FluxoPublicoController`;
3. persistir todos os contextos sociais pendentes do cadastro;
4. iterar todos os contextos em `finalizarCadastroPublico(...)`;
5. só depois ajustar o fluxo `/sessoes/sociais` para `entrar e vincular`;
6. por último, fechar a persistência pública da foto local do dispositivo.

## O que não deve ser alterado semanticamente

- `avatar_preferido_*` continua por projeto;
- `urlAvatarExterno = null` continua válido para Apple ou outro provedor sem
  foto;
- Apple sem foto continua podendo estar vinculada;
- escolher Google como avatar não pode apagar Apple como vínculo;
- escolher foto do dispositivo não pode apagar vínculos sociais.
