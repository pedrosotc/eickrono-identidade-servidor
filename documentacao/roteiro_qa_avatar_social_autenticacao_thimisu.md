# Roteiro QA - Autenticacao social, avatar e cache local Thimisu

## Objetivo

Validar em HML, pelo app Thimisu em iPhone fisico, os fluxos alterados de:

- cadastro comum;
- cadastro iniciado por rede social;
- vinculo social confirmado;
- avatar preferido;
- upload de foto local;
- login por senha;
- login social;
- biometria por conta;
- cache local de contas recentes;
- remocao do modelo antigo de contexto social pendente persistido no backend.

Este roteiro foi escrito para QA que nao conhece o app. Cada cenario deve ser
executado na ordem indicada, porque alguns casos dependem de usuarios criados
em cenarios anteriores.

## Escopo dos sistemas

| Sistema | Ambiente | Papel no teste |
| --- | --- | --- |
| App Thimisu | iPhone fisico HML | Executa cadastro, login, social, avatar e biometria |
| eickrono-autenticacao-servidor | HML | Recebe fluxos publicos, valida social, cria sessao e envia avatar para identidade |
| eickrono-identidade-servidor | HML | Persiste pessoa, avatar, upload e dados canonicos de identidade |
| Keycloak | HML | Autorizacao e identidade social ja vinculada |
| S3 | `eickrono-avatares-hml` | Armazenamento de avatar enviado pelo usuario |

## Decisao sobre base HML antes do QA

Base suja pode invalidar o teste. Versoes antigas podem ter deixado:

- contas locais sem `usuario`;
- vinculos sociais legados;
- avatares preferidos duplicados;
- contextos sociais pendentes persistidos;
- usuarios federados orfaos no Keycloak;
- cache local incompatibilizado no iPhone.

### Opcoes de preparacao

| Opcao | Quando usar | Risco | Recomendacao |
| --- | --- | --- | --- |
| A - Apagar HML inteiro | Quando HML e descartavel para todos | Alto | Usar somente com aprovacao explicita |
| B - Limpeza seletiva dos usuarios QA | Quando existem outros testes em HML | Medio | Preferida para este roteiro |
| C - Usar e-mails totalmente novos | Quando nao ha acesso seguro ao banco | Baixo | Boa para smoke, mas nao valida sujeira antiga |

Decisao recomendada para esta rodada:

1. Fazer limpeza seletiva dos e-mails e provedores usados pelo QA.
2. Reinstalar o app no iPhone antes do C01.
3. Manter uma rodada final com base suja controlada para validar migracao.

Nao apagar HML inteiro sem confirmacao explicita.

## Massa de dados de QA

Antes de iniciar, o QA deve preencher esta tabela com dados reais usados no
teste. Nao registrar senha em documento compartilhado.

| Identificador | Finalidade | Usuario | E-mail | Provedor | Observacao |
| --- | --- | --- | --- | --- | --- |
| QA-A | Cadastro comum sem foto |  |  | Nenhum | C01 |
| QA-B | Cadastro comum com foto local |  |  | Nenhum | C02 |
| QA-C | Cadastro iniciado por social sem foto social |  |  | Apple QA 01 | C03, C06 |
| QA-D | Cadastro social com avatar social preferido e foto local nao preferida |  |  | Google QA 01 | C04, C06, C13, C14 |
| QA-E | Cadastro social com foto local preferida |  |  | Google QA 02 | C05, C06 |
| QA-F | Conta local existente para entrar-e-vincular |  |  | Apple QA 01 ou Google QA 02 | C07, C08, C09 |
| QA-G | Conta desabilitada |  |  | Senha/social | C17 |
| QA-H | Conta nao liberada |  |  | Senha/social | C16 |

### Distribuicao pratica das redes sociais disponiveis

Esta rodada considera a massa real disponivel para o iPhone fisico:

| Alias | Rede | Quantidade disponivel | Foto social disponivel | Uso principal |
| --- | --- | --- | --- | --- |
| Apple QA 01 | Apple ID no iPhone | 1 conta | Nao | Cenarios sociais sem avatar social |
| Google QA 01 | Google/Gmail | 1 conta | Sim | Cenarios que precisam validar avatar social preferido |
| Google QA 02 | Google/Gmail | 1 conta | Sim | Cenarios que precisam de segunda identidade social, conflito ou avatar local preferido |

Regras desta rodada:

- Cenarios que precisam provar foto vinda da rede social devem usar Google.
- Cenarios com Apple nao devem esperar foto social; o resultado correto e
  sigla, foto local ou ausencia de avatar social, conforme o cenario.
- A mesma conta social nao deve ser usada em dois cadastros definitivos ao
  mesmo tempo.
- Quando um cenario precisar reutilizar a mesma conta social, executar antes a
  limpeza seletiva do usuario correspondente nos servidores.
- Se Apple QA 01 for usado em C03 e depois for necessario usa-lo em C07, o
  cadastro anterior deve ser removido ou o C07 deve usar Google QA 02.

### Mapa de execucao com as contas sociais disponiveis

| Cenario | Precisa rede social? | Conta/rede recomendada | Foto esperada |
| --- | --- | --- | --- |
| C01 | Nao | Nenhuma | Sigla do usuario |
| C02 | Nao | Nenhuma | Foto local THIMISU |
| C03 | Sim | Apple QA 01 | Sem foto social; usar sigla se nao houver foto local |
| C04 | Sim | Google QA 01 | Foto social Google como preferida |
| C05 | Sim | Google QA 02 | Foto local THIMISU como preferida; foto Google nao deve aparecer como preferida |
| C06 | Sim | Reusar QA-C, QA-D ou QA-E conforme o login validado | Conforme avatar preferido da conta reusada |
| C07 | Sim | Conta local QA-F + provedor ainda nao vinculado, preferir Apple QA 01 se estiver livre | Conforme conta local; social apenas vincula |
| C08 | Sim | Mesmo provedor preparado para C07 | Nao deve criar avatar/vinculo definitivo |
| C09 | Sim | Mesmo provedor preparado para C07 | Nao deve criar avatar/vinculo definitivo |
| C10 | Nao obrigatorio | Conta criada em C01, C02 ou C03 | Conforme conta selecionada |
| C11 | Nao obrigatorio | Conta criada em C01, C02 ou C03 | Conforme conta selecionada |
| C12 | Nao obrigatorio | Duas contas locais ja criadas | Conforme cada conta |
| C13 | Depende | Preferir QA-D ou QA-E | Deve atualizar conforme avatar preferido alterado |
| C14 | Sim | QA-D com Google QA 01 | Avatar social removido/indisponivel deve cair para sigla ou outro preferido |
| C15 | Sim | Apple QA 01 ou Google QA 02 | Nao deve persistir avatar/vinculo em falha de rede |
| C16 | Nao obrigatorio | Cadastro pendente por senha | Sem requisito de foto |
| C17 | Nao obrigatorio | Conta desabilitada por senha ou social | Sem requisito de foto |
| C18 | Sim | Google QA 01 ja vinculado + conta local diferente | Conflito; nao deve transferir avatar/vinculo |
| C19 | Depende da sujeira preparada | Usar qualquer conta preparada para regressao | Conforme dado legado controlado |

## Preparacao tecnica obrigatoria

Executar antes do QA manual.

| Ordem | Acao | Resultado esperado |
| --- | --- | --- |
| P01 | Confirmar `identidade-hml` atualizado | ECS `COMPLETED` e `/api/v1/estado` HTTP 200 |
| P02 | Confirmar `auth-hml` atualizado | ECS `COMPLETED` e runtime HTTP 200 |
| P03 | Confirmar app HML instalado no iPhone fisico | App abre no aparelho |
| P04 | Confirmar bucket `eickrono-avatares-hml` acessivel | Upload de avatar nao falha por permissao |
| P05 | Confirmar logs CloudWatch dos dois servidores | Busca por `qa_` retorna eventos quando fluxo roda |
| P06 | Reinstalar app no iPhone | Cache local inicia limpo |
| P07 | Fazer limpeza seletiva da massa QA | Usuarios QA iniciam sem dados conflitantes |

## Evidencias obrigatorias por cenario

Para cada cenario, registrar:

| Evidencia | Como coletar |
| --- | --- |
| Horario de inicio e fim | Relogio local do QA |
| Prints do app | Antes da acao principal e depois do resultado |
| Logs do app | Console filtrando por `qa_` |
| Logs do autenticacao-servidor | CloudWatch filtrando por `qa_` |
| Logs do identidade-servidor | CloudWatch filtrando por `qa_` |
| Consultas SQL quando aplicavel | Usar queries deste roteiro |
| Provedor usado | Google, Apple ou nenhum |

Proibido registrar token social, senha, e-mail completo em texto livre ou URL
assinada sensivel.

## Logs diagnosticos esperados

### App Thimisu

| Log | Significado |
| --- | --- |
| `qa_login_social_inicio` | Usuario tocou em Google/Apple |
| `qa_login_social_credencial_nativa` | Credencial social voltou do provedor |
| `qa_login_social_pendente_registrado_local` | App guardou contexto social somente localmente |
| `qa_login_social_vinculo_confirmado_app` | App tentou concluir vinculo social apos login local |
| `qa_cadastro_payload_social_avatar` | App montou listas de vinculos e avatares para cadastro |
| `qa_cadastro_publico_enviado` | App enviou cadastro publico |
| `qa_conta_local_cache_salvo` | Conta local foi gravada/atualizada no catalogo local |
| `qa_conta_local_cache_restaurado` | Conta local foi restaurada/reconciliada |

### eickrono-autenticacao-servidor

| Log | Significado |
| --- | --- |
| `qa_login_social_backend_recebido` | Backend recebeu tentativa social |
| `qa_login_social_credencial_validada` | Credencial social foi validada |
| `qa_login_social_sem_vinculo_definitivo` | Social valido, mas sem conta/vinculo definitivo |
| `qa_login_social_vinculo_definitivo_encontrado` | Social ja estava vinculado e login pode concluir |
| `qa_cadastro_publico_recebido` | Cadastro chegou ao backend |
| `qa_cadastro_publico_normalizado` | Payload social/avatar foi normalizado |
| `qa_cadastro_pendente_criado` | Cadastro pendente foi criado |
| `qa_cadastro_confirmados_registrados` | Vinculos e avatares confirmados foram registrados |
| `qa_cadastro_email_consumo_confirmados_inicio` | Confirmacao de e-mail iniciou consumo |
| `qa_cadastro_email_consumo_confirmados_fim` | Confirmacao de e-mail terminou consumo |
| `qa_avatar_upload_identidade_inicio` | Autenticacao iniciou envio de avatar para identidade |
| `qa_avatar_upload_identidade_fim` | Identidade retornou dados do avatar |
| `qa_sessao_publica_avatar_resolvido` | Sessao retornou avatar preferido ou ausencia |

### eickrono-identidade-servidor

| Log | Significado |
| --- | --- |
| `qa_avatar_upload_interno_recebido` | Endpoint interno recebeu upload |
| `qa_avatar_upload_validado` | Payload da imagem foi validado |
| `qa_avatar_storage_s3_inicio` | Upload no S3 iniciou |
| `qa_avatar_storage_s3_fim` | Upload no S3 terminou |
| `qa_avatar_storage_local_fim` | Storage local terminou em ambiente local |

## Ordem de execucao dos cenarios

Este roteiro possui 18 cenarios principais, C01 a C18. O C19 e um cenario
extra de regressao com base suja controlada e deve ser executado somente quando
houver dados legados preparados.

| Ordem | Cenario | Fase | Dependencia |
| --- | --- | --- | --- |
| C01 | Cadastro comum sem foto | Base | Nenhuma |
| C02 | Cadastro comum com foto local preferida | Base | Nenhuma |
| C03 | Cadastro iniciado por social sem foto local | Social cadastro | Nenhuma |
| C04 | Cadastro social com avatar social preferido e foto local nao preferida | Social cadastro | C03 entendido |
| C05 | Cadastro social com foto local preferida | Social cadastro | C03 entendido |
| C06 | Login social ja vinculado | Login | C03, C04 ou C05 |
| C07 | Entrar e vincular com conta local existente | Vinculo | C01 ou C02 + provedor ainda nao vinculado |
| C08 | Entrar e vincular cancelado | Vinculo | C07 ate abrir aviso |
| C09 | Entrar e vincular com tres falhas | Vinculo | C07 ate abrir aviso |
| C10 | Conta recente e usuario obrigatorio | Cache | Login bem-sucedido de C01/C02/C03 |
| C11 | Biometria por conta | Cache/biometria | C10 |
| C12 | Biometria entre duas contas | Cache/biometria | Duas contas com biometria habilitada |
| C13 | Avatar atualizado no servidor | Avatar/cache | C02, C04 ou C05 |
| C14 | Avatar social removido/indisponivel | Avatar/cache | Conta com avatar social preferido |
| C15 | Falha de rede no login social | Falha | Nenhuma |
| C16 | Conta nao liberada | Falha funcional | Cadastro pendente |
| C17 | Conta desabilitada | Falha funcional | Conta preparada no backend |
| C18 | Conflito social com outra conta | Falha funcional | Rede social ja vinculada a outra conta |
| C19 | Base suja controlada | Regressao | Dados legados preparados |

## C01 - Cadastro comum sem rede social e sem foto

Objetivo:

Validar que cadastro sem rede social e sem foto nao cria vinculo social nem
avatar e que o app usa sigla do usuario.

Pre-condicao:

QA-A nao existe na base HML.

Passos:

1. Apagar o app do iPhone.
2. Instalar a build HML atualizada.
3. Abrir o app.
4. Na tela de login, tocar em `Ainda nao possui conta? Cadastre-se`.
5. Preencher usuario de QA-A.
6. Preencher e-mail de QA-A.
7. Preencher senha valida.
8. Preencher os dados obrigatorios do cadastro.
9. Nao selecionar foto de perfil.
10. Finalizar cadastro.
11. Validar e-mail pelo fluxo recebido.
12. Voltar ao app.
13. Fazer login por usuario e senha.

Esperado:

- cadastro conclui;
- login por senha funciona;
- app mostra sigla do usuario quando nao ha foto;
- sessao retorna sem avatar preferido;
- nenhum vinculo social e criado.

Logs obrigatorios:

- `qa_cadastro_payload_social_avatar` com `vinculos_sociais_confirmados=0`;
- `qa_cadastro_payload_social_avatar` com `avatares_cadastro_confirmados=0`;
- `qa_cadastro_publico_recebido`;
- `qa_sessao_publica_avatar_resolvido` com `avatarAusente=true`;
- `qa_conta_local_cache_salvo` com usuario presente.

## C02 - Cadastro comum com foto local preferida

Objetivo:

Validar upload de foto local do usuario para S3 e retorno do avatar preferido
na sessao.

Pre-condicao:

QA-B nao existe na base HML.

Passos:

1. Reinstalar app ou garantir que nao ha conta selecionada.
2. Abrir cadastro.
3. Preencher usuario, e-mail, senha e dados obrigatorios de QA-B.
4. Na etapa de foto de perfil, escolher `Tirar uma foto` ou selecionar imagem
   da galeria.
5. Confirmar a foto local como foto de perfil.
6. Finalizar cadastro.
7. Validar e-mail.
8. Fazer login por usuario e senha.
9. Conferir se a foto aparece no lugar da sigla.

Esperado:

- app envia um avatar de origem `THIMISU`;
- autenticacao-servidor envia upload para identidade;
- identidade grava arquivo no S3 HML;
- sessao retorna avatar preferido com URL e versao/hash/data;
- app mostra foto local.

Logs obrigatorios:

- `qa_cadastro_payload_social_avatar` com `avatares_cadastro_confirmados=1`;
- `qa_cadastro_payload_social_avatar` com `avatar_dispositivo_carregado=true`;
- `qa_cadastro_payload_social_avatar` com `avatar_dispositivo_preferido=true`;
- `qa_avatar_upload_identidade_inicio`;
- `qa_avatar_upload_interno_recebido`;
- `qa_avatar_upload_validado`;
- `qa_avatar_storage_s3_fim`;
- `qa_avatar_upload_identidade_fim`;
- `qa_sessao_publica_avatar_resolvido` com avatar presente.

## C03 - Cadastro iniciado por rede social sem foto local

Objetivo:

Validar que a rede social autenticada antes do cadastro fica temporaria no app,
sem persistir contexto social pendente no backend, e so vira vinculo definitivo
quando o cadastro e concluido.

Pre-condicao:

QA-C nao existe na base HML e o e-mail social usado nao pertence a conta local.
Nesta rodada, usar Apple QA 01 para validar o caminho social sem foto social.

Passos:

1. Reinstalar app.
2. Na tela de login, tocar em Apple.
3. Autenticar com a conta social QA-C.
4. Quando o app perguntar se deseja iniciar cadastro, tocar em `Sim`.
5. Conferir que a tela de cadastro abriu.
6. Preencher os dados obrigatorios restantes.
7. Nao selecionar foto local.
8. Finalizar cadastro.
9. Validar e-mail.
10. Sair do app ou voltar ao login.
11. Tocar novamente no mesmo provedor social.

Esperado:

- antes do cadastro, backend nao cria contexto social pendente persistido;
- app guarda dados sociais temporariamente;
- cadastro envia lista de vinculos sociais confirmados;
- apenas uma opcao de avatar pode ficar preferida;
- como Apple nao disponibiliza foto social nesta massa, nao esperar avatar
  social no cadastro nem na home;
- login social posterior entra direto.

Logs obrigatorios:

- `qa_login_social_inicio`;
- `qa_login_social_credencial_nativa`;
- `qa_login_social_backend_recebido`;
- `qa_login_social_credencial_validada`;
- `qa_login_social_sem_vinculo_definitivo`;
- `qa_login_social_pendente_registrado_local`;
- `qa_cadastro_payload_social_avatar` com `vinculos_sociais_confirmados=1`;
- `qa_cadastro_confirmados_registrados`;
- `qa_cadastro_email_consumo_confirmados_fim`;
- no login posterior, `qa_login_social_vinculo_definitivo_encontrado`.

## C04 - Cadastro social com avatar social preferido e foto local nao preferida

Objetivo:

Validar que foto local enviada tambem vira opcao de avatar, mas somente o avatar
social fica preferido.

Pre-condicao:

QA-D nao existe na base HML. Nesta rodada, usar Google QA 01 porque este
cenario precisa validar foto social.

Passos:

1. Iniciar cadastro por Google com QA-D.
2. Aceitar abrir cadastro.
3. Carregar foto local do dispositivo.
4. Selecionar o avatar social como preferido.
5. Finalizar cadastro.
6. Validar e-mail.
7. Fazer login social.

Esperado:

- vinculo social definitivo e criado;
- foto local e enviada e armazenada;
- avatar social fica `preferido=true`;
- foto local fica `preferido=false`;
- consulta de banco nao retorna dois avatares preferidos.

Logs obrigatorios:

- `qa_cadastro_payload_social_avatar` com `vinculos_sociais_confirmados=1`;
- `qa_cadastro_payload_social_avatar` com `avatares_cadastro_confirmados=1`;
- `qa_cadastro_payload_social_avatar` com `avatar_social_preferido=true`;
- `qa_cadastro_payload_social_avatar` com `avatar_dispositivo_preferido=false`;
- `qa_avatar_storage_s3_fim`;
- `qa_sessao_publica_avatar_resolvido`.

## C05 - Cadastro social com foto local preferida

Objetivo:

Validar que a rede social vira forma de acesso, mas a foto local `THIMISU`
fica como avatar preferido.

Pre-condicao:

QA-E nao existe na base HML. Nesta rodada, usar Google QA 02 para separar este
cadastro do Google QA 01 usado em C04/C14.

Passos:

1. Iniciar cadastro por Google com QA-E.
2. Aceitar abrir cadastro.
3. Carregar foto local.
4. Selecionar a foto local como preferida.
5. Finalizar cadastro.
6. Validar e-mail.
7. Fazer login social com o mesmo provedor.
8. Conferir que a foto local aparece, nao a foto social.

Esperado:

- rede social fica vinculada;
- avatar local fica preferido;
- sessao retorna origem `THIMISU`;
- login social entra direto e usa avatar local preferido.

Logs obrigatorios:

- `qa_cadastro_payload_social_avatar` com `avatar_dispositivo_preferido=true`;
- `qa_avatar_upload_identidade_fim`;
- `qa_login_social_vinculo_definitivo_encontrado`;
- `qa_sessao_publica_avatar_resolvido` com avatar presente.

## C06 - Login social ja vinculado

Objetivo:

Validar o caso perfeito do login social: rede ja vinculada ao usuario correto
entra direto.

Pre-condicao:

Usar QA-C, QA-D ou QA-E ja criado por cadastro social.

Passos:

1. Abrir app na tela de login.
2. Tocar no mesmo provedor social usado no cadastro.
3. Concluir autenticacao social.
4. Observar se o app entra na home.

Esperado:

- app entra direto;
- nao pergunta cadastro;
- nao pergunta entrar-e-vincular;
- nao abre tela de excecao;
- nao cria novo usuario.

Observacao de massa:

- se usar QA-C, nao esperar avatar social porque Apple QA 01 nao fornece foto;
- se usar QA-D, esperar foto social Google;
- se usar QA-E, esperar foto local THIMISU, porque ela foi marcada como
  preferida.

Logs obrigatorios:

- `qa_login_social_vinculo_definitivo_encontrado`;
- `qa_sessao_publica_avatar_resolvido`;
- `qa_conta_local_cache_salvo`.

## C07 - Entrar e vincular com conta local existente

Objetivo:

Validar conta local existente com mesmo e-mail social e sem vinculo social.

Pre-condicao:

QA-F existe como conta local por senha e o provedor social QA-F ainda nao esta
vinculado.
Nesta rodada, preferir Apple QA 01 se ele estiver livre. Se Apple QA 01 ja foi
usado em C03 e nao foi limpo, usar Google QA 02 somente se ele tambem estiver
livre; nunca reutilizar uma rede ja vinculada sem limpeza seletiva.

Passos:

1. Abrir app na tela de login.
2. Tocar no provedor social escolhido para QA-F usando o mesmo e-mail da conta
   local.
3. Aguardar aviso inferior perguntando se deseja vincular.
4. Escolher `Entrar e vincular`.
5. Fazer login com usuario e senha de QA-F.
6. Nao reautenticar no provedor social.
7. Sair e tentar login social novamente.

Esperado:

- app nao pede segunda autenticacao social;
- login local conclui vinculo social;
- login social posterior entra direto;
- contexto temporario e limpo apos sucesso.

Logs obrigatorios:

- `qa_login_social_sem_vinculo_definitivo`;
- `qa_login_social_pendente_registrado_local`;
- `qa_login_social_vinculo_confirmado_app`;
- `qa_login_social_vinculo_definitivo_encontrado` no login posterior.

## C08 - Entrar e vincular cancelado

Objetivo:

Validar que cancelar o vinculo descarta o contexto social temporario.

Pre-condicao:

Repetir C07 ate aparecer o aviso inferior.

Passos:

1. No aviso inferior, tocar em `Agora nao`.
2. Fazer login normal com usuario e senha.
3. Sair.
4. Tentar login social novamente.

Esperado:

- aviso fecha;
- app nao vincula rede social ao login normal;
- login social posterior volta a perguntar, porque ainda nao ha vinculo;
- nenhum dado temporario vira definitivo.

Logs obrigatorios:

- `qa_login_social_pendente_registrado_local`;
- nao deve aparecer `qa_login_social_vinculo_confirmado_app` apos cancelar.

## C09 - Entrar e vincular com tres falhas de senha

Objetivo:

Validar descarte do contexto social temporario apos tres erros de senha.

Pre-condicao:

Repetir C07 ate aparecer o aviso inferior.

Passos:

1. Escolher `Entrar e vincular`.
2. Digitar senha errada uma vez.
3. Digitar senha errada segunda vez.
4. Digitar senha errada terceira vez.
5. Depois digitar senha correta.
6. Sair e tentar login social.

Esperado:

- apos tres falhas, contexto social temporario e descartado;
- senha correta posterior nao vincula rede social;
- login social posterior ainda nao entra direto.

Logs obrigatorios:

- `qa_login_social_pendente_registrado_local`;
- nao deve haver vinculo definitivo depois das tres falhas;
- nao deve aparecer `qa_login_social_vinculo_confirmado_app` apos descarte.

## C10 - Conta recente e usuario obrigatorio

Objetivo:

Validar lista de contas recentes, usuario obrigatorio e e-mail mascarado.

Pre-condicao:

Pelo menos uma conta fez login com sucesso em C01, C02 ou C03.

Passos:

1. Sair da conta.
2. Na tela de login, tocar no campo `E-mail ou usuario`.
3. Confirmar que abre o `showModalBottomSheet` de contas recentes.
4. Conferir item da lista.
5. Tocar na conta recente.
6. Conferir campo de login preenchido.
7. Alterar um caractere do campo.

Esperado:

- lista aparece somente no bottom sheet;
- primeira linha do item e `usuario` em destaque;
- segunda linha e e-mail mascarado;
- ao selecionar, campo recebe `usuario`, nao e-mail;
- se campo for editado manualmente, botao de biometria some;
- conta sem usuario nao aparece.

Logs obrigatorios:

- `qa_conta_local_cache_salvo` com `usuario_presente=true` ou
  `usuarioPresente=true`;
- se aparecer `usuario_presente=false`, abrir bug de contrato/cache.

## C11 - Biometria por conta

Objetivo:

Validar que biometria so aparece quando a conta selecionada tem biometria
habilitada e sessao segura compatibilizada com a conta.

Pre-condicao:

Conta QA-A, QA-B ou QA-C tem login bem-sucedido e convite biometrico pendente.

Passos:

1. Fazer login por senha.
2. No convite pos-login, aceitar biometria.
3. Sair do app.
4. Abrir login.
5. Selecionar conta recente no bottom sheet.
6. Verificar se botao biometria aparece.
7. Tocar em biometria.
8. Autenticar no Face ID/Touch ID.

Esperado:

- biometria aparece para conta habilitada;
- biometria nao aparece para conta com estado `false` ou `null`;
- login biometrico entra na conta selecionada;
- nao aparece `Login nao encontrado para biometria`.

Logs obrigatorios:

- `qa_conta_local_cache_salvo` com `biometria_estado=habilitada` ou
  `biometria=habilitada`;
- `qa_conta_local_cache_restaurado`.

## C12 - Biometria entre duas contas

Objetivo:

Validar que cada conta usa sua propria sessao biometrica e que uma conta nao
depende da sessao segura de outra.

Pre-condicao:

Duas contas diferentes existem no mesmo iPhone e ambas tiveram biometria
habilitada.

Passos:

1. Fazer login na conta 1.
2. Aceitar biometria.
3. Sair.
4. Fazer login na conta 2.
5. Aceitar biometria.
6. Sair.
7. Abrir lista de contas recentes.
8. Selecionar conta 1.
9. Entrar por biometria.
10. Sair.
11. Selecionar conta 2.
12. Entrar por biometria.

Esperado:

- botao biometria aparece para as duas contas;
- cada biometria entra na conta correta;
- trocar conta nao reaproveita credencial errada;
- nao aparece erro de login nao encontrado.

Logs obrigatorios:

- `qa_conta_local_cache_salvo` para as duas contas;
- `qa_conta_local_cache_restaurado` ao abrir login.

## C13 - Avatar atualizado no servidor

Objetivo:

Validar regra de atualizacao do cache local quando avatar muda no servidor.

Pre-condicao:

Conta QA-B, QA-D ou QA-E possui avatar preferido.

Passos:

1. Fazer login e confirmar avatar atual.
2. Alterar avatar preferido no servidor por fluxo suportado.
3. Sair do app.
4. Fazer login novamente.
5. Conferir se avatar mudou no app.

Esperado:

- se versao/hash/data mudou, app atualiza cache;
- se versao/hash/data nao mudou, app preserva cache;
- se servidor retorna sem avatar, app usa sigla.

Logs obrigatorios:

- `qa_sessao_publica_avatar_resolvido`;
- `qa_conta_local_cache_salvo`.

## C14 - Avatar social removido ou indisponivel

Objetivo:

Validar comportamento quando avatar social preferido deixa de existir.

Pre-condicao:

Conta com avatar social preferido.

Passos:

1. Confirmar que o avatar social aparece no app.
2. Remover ou invalidar URL social no servidor, por procedimento controlado.
3. Fazer login novamente.

Esperado:

- app nao quebra;
- se servidor retorna ausencia, app mostra sigla;
- se servidor retorna outro preferido, app mostra novo avatar;
- cache local nao preserva avatar antigo quando versao indica remocao.

Logs obrigatorios:

- `qa_sessao_publica_avatar_resolvido` com `avatarAusente=true` quando
  aplicavel;
- `qa_conta_local_cache_salvo`.

## C15 - Falha de rede no login social

Objetivo:

Validar falha temporaria de rede sem persistir estado definitivo.

Pre-condicao:

Nenhuma.

Passos:

1. Abrir tela de login.
2. Desativar internet do iPhone ou bloquear acesso aos endpoints HML.
3. Tocar em Google ou Apple.
4. Observar mensagem no app.
5. Reativar rede.
6. Tentar novamente.

Esperado:

- app mostra mensagem inferior temporaria;
- nao abre cadastro;
- nao salva vinculo definitivo;
- apos rede voltar, fluxo funciona normalmente.

Logs obrigatorios:

- o fluxo deve parar antes de `qa_login_social_credencial_validada` ou registrar
  erro de chamada ao backend;
- nao deve aparecer `qa_cadastro_confirmados_registrados`.

## C16 - Conta nao liberada

Objetivo:

Validar conta ainda nao liberada e retomada de validacao.

Pre-condicao:

Cadastro pendente de QA-H com validacao incompleta.

Passos:

1. Tentar login com QA-H.
2. Observar aviso inferior.
3. Tocar em `Nao`.
4. Confirmar que aviso fecha e app permanece no login.
5. Tentar login novamente.
6. Tocar em `Sim`.
7. Confirmar navegacao para `/validacao-contatos`.

Esperado:

- app mostra aviso inferior com decisao;
- `Nao` apenas fecha aviso;
- `Sim` retoma validacao;
- nao promove conta como login concluido.

Logs obrigatorios:

- logs de erro funcional do login;
- nao deve haver `qa_conta_local_cache_salvo` como conta autenticada pronta.

## C17 - Conta desabilitada

Objetivo:

Validar usuario bloqueado.

Pre-condicao:

QA-G esta desabilitado no backend.

Passos:

1. Tentar login por senha com QA-G.
2. Voltar para login.
3. Se QA-G tiver social vinculado, tentar login social.

Esperado:

- app abre tela de excecao de usuario bloqueado;
- botao voltar retorna ao login;
- nenhum cache novo e promovido;
- social tambem nao entra.

Logs obrigatorios:

- erro funcional de conta desabilitada;
- nao deve haver `qa_conta_local_cache_salvo` como login concluido.

## C18 - Conflito social com outra conta

Objetivo:

Validar que uma rede social vinculada ao usuario A nao pode ser vinculada ao
usuario B.

Pre-condicao:

Rede social ja vinculada a QA-C ou QA-D.
Nesta rodada, preferir Google QA 01 ja vinculado em QA-D para validar conflito
com foto social e impedir transferencia indevida de avatar/vinculo.

Passos:

1. Criar ou usar conta local QA-F diferente.
2. Entrar no fluxo de vinculo social com a mesma rede ja vinculada a QA-C/QA-D.
3. Tentar concluir vinculo.

Esperado:

- backend rejeita conflito;
- rede continua vinculada somente ao usuario original;
- app mostra mensagem inferior temporaria ou fluxo de erro definido;
- conta B nao recebe forma social.

Logs obrigatorios:

- log de conflito funcional no autenticacao-servidor;
- nao deve haver novo vinculo definitivo para usuario B.

## C19 - Base suja controlada

Objetivo:

Validar comportamento com dados antigos ou inconsistentes.

Pre-condicao:

Ambiente preparado com pelo menos uma destas sujeiras controladas:

- conta local antiga sem `usuario`;
- avatar preferido duplicado;
- vinculo social legado;
- contexto social pendente antigo;
- usuario federado orfao no Keycloak.

Passos:

1. Abrir app com build atual.
2. Tocar no campo de login para abrir contas recentes.
3. Fazer login por senha com conta corrigida.
4. Fazer login social com rede conhecida.
5. Consultar banco depois do fluxo.

Esperado:

- conta sem usuario nao aparece em contas recentes;
- login novo com sessao completa grava usuario corretamente;
- fluxo novo nao cria contexto social pendente;
- avatar duplicado e tratado como erro funcional ou corrigido por regra de
  preferido unico;
- qualquer excecao deve ser registrada para suporte corrigir base.

Logs obrigatorios:

- `qa_conta_local_cache_restaurado`;
- `qa_conta_local_cache_salvo`;
- logs de erro funcional quando houver dado inconsistente.

## Consultas SQL de apoio

Adaptar nomes de schema conforme o banco HML em uso.

```sql
-- Deve existir no maximo um avatar preferido por usuario_cliente.
SELECT usuario_cliente_id, COUNT(*) AS preferidos
FROM identidade.avatar_usuario
WHERE preferido IS TRUE
  AND removido_em IS NULL
GROUP BY usuario_cliente_id
HAVING COUNT(*) > 1;
```

```sql
-- Deve existir forma de acesso social definitiva apos cadastro/vinculo.
SELECT usuario_id, tipo, provedor, identificador_externo, desvinculado_em
FROM autenticacao.usuarios_formas_acesso
WHERE tipo = 'SOCIAL'
ORDER BY vinculado_em DESC;
```

```sql
-- Deve existir avatar THIMISU quando foto local foi enviada.
SELECT avatar.id,
       origem.codigo,
       avatar.url_avatar,
       avatar.storage_key,
       avatar.hash_conteudo,
       avatar.versao,
       avatar.preferido
FROM identidade.avatar_usuario avatar
JOIN identidade.avatar_origens origem ON origem.id = avatar.origem_id
WHERE origem.codigo = 'THIMISU'
ORDER BY avatar.atualizado_em DESC;
```

```sql
-- Nao deve haver conta recente valida sem usuario no app.
-- Esta validacao e local no SQLite/Drift do app, nao no banco HML.
-- Se QA encontrar usuario nulo no cache local, coletar log
-- qa_conta_local_cache_salvo e abrir bug de contrato/cache.
```

## Criterios de aprovacao

O QA pode aprovar a rodada quando:

- C01 a C18 passam em ordem;
- C19 passa ou gera lista controlada de dados legados a corrigir;
- logs `qa_` aparecem nos pontos esperados;
- nenhum fluxo novo persiste contexto social pendente no backend;
- cadastro nao permite mais de um avatar preferido;
- login por senha, login social e biometria convergem para a mesma conta;
- avatar preferido da sessao e refletido no cache local;
- reinstalacao do app nao recria inconsistencias de usuario/avatar/cache;
- falhas funcionais nao deixam vinculo/avatar definitivo indevido.

## Triage rapida

| Sintoma | Primeira verificacao |
| --- | --- |
| Conta recente nao aparece | `qa_conta_local_cache_salvo` com `usuario_presente` ou `usuarioPresente` |
| Avatar nao aparece | `qa_sessao_publica_avatar_resolvido` e dados de avatar presente/ausente |
| Upload local falha | `qa_avatar_upload_identidade_inicio`, `qa_avatar_upload_interno_recebido`, `qa_avatar_storage_s3_fim` |
| Login social abre cadastro indevidamente | Ausencia de `qa_login_social_vinculo_definitivo_encontrado` |
| Entrar-e-vincular pede social de novo | Ver `qa_login_social_vinculo_confirmado_app` e contexto local |
| Biometria aparece para conta errada | Ver cache por conta e estado `biometria_estado` |
| Campo login recebe e-mail | Bug no apresentador/selecao de conta recente; deve receber `usuario` |
| Contexto social pendente voltou | Bug arquitetural; fluxo novo nao deve persistir pendencia social |

## Regra de manutencao deste roteiro

Sempre que o comportamento aprovado mudar, atualizar primeiro este documento e
so depois alterar codigo ou executar QA. O QA deve comparar o resultado real
com este roteiro, nao com memoria de conversas anteriores.
