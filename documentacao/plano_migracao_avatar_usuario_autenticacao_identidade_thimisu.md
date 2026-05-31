# Plano de migracao - avatar do usuario, redes sociais e sessao autenticada

> Status: implementacao parcial em andamento.
>
> Escopo: `eickrono-autenticacao-servidor`,
> `eickrono-identidade-servidor`, `eickrono-autenticacao-cliente` e
> `eickrono-thimisu-app`.
>
> Este documento nao executa alteracoes por si so. Ele define o alvo, as etapas
> de implementacao, as migrations previstas e os testes obrigatorios.

## Objetivo

Corrigir o desenho de avatar e redes sociais para que:

- autenticacao social antes de cadastro ou vinculo nao grave usuario, rede
  social, avatar ou contexto pendente no banco;
- a rede social so seja persistida depois que existir pessoa/usuario definitivo;
- o app Thimisu consiga receber, exibir e atualizar a foto de perfil preferida;
- foto vinda de rede social e foto carregada pelo dispositivo sejam tratadas
  como opcoes de avatar do mesmo usuario;
- a escolha visual de avatar nao defina qual rede social e a principal;
- todos os projetos usem uma fonte canonica para pessoa, e-mail, telefone,
  forma de acesso e avatar.

## Restricao atual de identidade canonica

O DBML alvo usa `identidade.pessoas.id` como `UUID`, mas o runtime ainda possui
contratos e tabelas que carregam o identificador numerico legado da pessoa.

Pontos afetados:

- `ContextoPessoaPerfilSistema.pessoaId` ainda e `Long`;
- respostas internas de confirmacao de pessoa ainda retornam `pessoaId` `Long`;
- provisionamento de produto ainda envia `pessoaIdCentral` `Long`;
- tabelas antigas de cadastro, dispositivo, atestacao e vinculo organizacional
  ainda usam `pessoa_id_perfil BIGINT`;
- alguns fallbacks de contexto ainda usam `ClienteContextoPessoaPerfilSistemaLegado`.

Regra para esta migracao:

1. O modelo de avatar pode usar `UUID` como alvo final no schema.
2. O codigo de runtime nao deve assumir que a migracao `Long` -> `UUID` ja foi
   concluida.
3. Enquanto existir `pessoaIdCentral` numerico nos contratos, respostas de
   sessao/avatar devem manter compatibilidade com esse identificador.
4. O proximo corte seguro e adicionar `pessoaCanonicaId UUID` em paralelo aos
   campos numericos, migrar consumidores por etapas e remover o `Long` somente
   depois que login, dispositivo, biometria, cadastro e provisionamento estiverem
   validados.

Consequencia pratica:

- nao dropar `pessoas_identidade`, `perfis_identidade`,
  `pessoas_formas_acesso`, `vinculos_sociais` ou colunas `pessoa_id_perfil`
  apenas porque o DBML alvo ja descreve o modelo novo;
- nao alterar `ContextoPessoaPerfilSistema.pessoaId` diretamente para `UUID`
  sem uma migracao coordenada nos dois servidores e nos backends de produto;
- novas consultas devem preferir schema explicito e modelo canonico quando o
  contrato ja permitir, mas fallbacks legados devem permanecer isolados ate a
  etapa de remocao.

## Dependencias com exclusao de cadastro/produto

Este plano depende da especificacao administrativa de exclusao de acesso e
conta de produto:

`eickrono-autenticacao-servidor/documentacao/especificacao_servico_exclusao_usuario_cadastro_produto.md`

A relacao entre os dois documentos e a seguinte:

Regra de integracao:

- o servico administrativo de exclusao/reset resolve o alvo pelo produto:
  `produto + usuarioPublicoProduto` ou `produto + perfilProdutoId`;
- e-mail, `sub`, usuario de autenticacao e provedor social entram apenas como
  chaves auxiliares depois que o alvo de produto estiver identificado;
- avatar social/local removido por esse servico deve pertencer ao usuario de
  acesso e ao produto resolvido, nunca a outro produto nem a configuracao global
  de provedor social.

| Frente | Pode avancar agora? | Condicao |
| --- | --- | --- |
| Cadastro/login social com dados temporarios no app | Sim. | O app mantem dados sociais temporarios ate cadastro/vinculo final; os servidores so gravam vinculo social definitivo quando ja existir usuario/pessoa definitivos. |
| Avatar social e avatar local controlado | Sim. | O contrato de sessao deve continuar compativel com `pessoaIdCentral Long` enquanto a migracao canonica nao terminar. |
| Cache de avatar no app | Sim. | O app deve usar `versao` como principal e `url_avatar`/`atualizado_em` como fallback para decidir atualizacao local. |
| Remocao fisica de avatar no storage | Sim, dentro do pacote de exclusao de avatar. | Antes de remover/desassociar o registro logico, o servico precisa materializar `bucket` e `storageKey` em pendencia/auditoria imutavel. |
| Drop da tabela de pendencia social em servidor | Sim, se nenhum fluxo funcional novo depender da tabela. | Deve ser feito por migration nova e validado por busca de codigo/testes. |
| Drop de tabelas/colunas de pessoa legado | Nao agora. | Bloqueado ate existir `pessoaCanonicaId UUID` em paralelo, consumidores migrados e validacao completa. |
| Troca direta de `ContextoPessoaPerfilSistema.pessoaId` para `UUID` | Nao. | Deve ser feita por campo paralelo, nao por substituicao direta. |

Ordem recomendada de execucao:

1. Fechar o fluxo social/avatar confirmado sem gravar contexto social pendente.
2. Implementar resolucao e `dryRun` do servico administrativo de exclusao.
3. Implementar remocao/desassociacao de avatar com pendencia imutavel de
   `storageKey`.
4. Adicionar `pessoaCanonicaId UUID` em paralelo aos contratos e tabelas que
   ainda usam `pessoaIdCentral Long`.
5. Migrar leituras para preferirem `pessoaCanonicaId`, mantendo fallback
   temporario.
6. Remover fallbacks, colunas e tabelas legadas apenas depois que os fluxos de
   cadastro, login, social, dispositivo, biometria, avatar e exclusao estiverem
   validados.

Regra operacional:

- antes de codificar qualquer etapa, revisar este documento e a especificacao
  do servico de exclusao para confirmar se a mudanca pertence ao pacote atual;
- se a mudanca exigir remover legado fisico, confirmar primeiro se ela depende
  do pacote `pessoaCanonicaId UUID`;
- se a mudanca apenas isola legado sem apagar estrutura usada, ela pode entrar
  em pacote anterior.

## Regras alvo

### 0. Storage dedicado para avatares

As imagens de avatar controladas pela Eickrono devem usar buckets S3
dedicados por ambiente:

- `eickrono-avatares-hml`
- `eickrono-avatares-prod`

Motivo:

- avatar tem politica propria de leitura publica/controlada;
- avatar precisa de cache/CDN com regra independente de outros arquivos;
- avatar pode ter limpeza, versionamento e invalidacao sem afetar documentos,
  backups ou arquivos de produto;
- criar o bucket dedicado desde o inicio evita migracao posterior de objetos,
  URLs, permissoes e cache.

Regra de persistencia:

- o app nunca deve depender do nome do bucket;
- o app recebe apenas `url_avatar` e metadados de versao/cache;
- o banco pode guardar `storage_key` para operacao interna do backend;
- `storage_key` deve ser relativo ao bucket, por exemplo
  `usuarios/{usuarioId}/avatares/{origem}/{arquivo}`;
- em HML, `storage_key` resolve dentro de `eickrono-avatares-hml`;
- em producao, `storage_key` resolve dentro de `eickrono-avatares-prod`.

### 1. Autenticacao social antes de cadastro ou vinculo

Quando o usuario toca em Google ou Apple no app:

1. O app executa a autenticacao nativa.
2. O `eickrono-autenticacao-servidor` valida a credencial social.
3. Se ainda nao existe conta local definitiva para concluir o login, o servidor
   devolve ao app os dados sociais validados.
4. Nenhum registro definitivo deve ser criado nesse momento.

Proibido nesse passo:

- criar usuario no Keycloak;
- criar usuario em banco local;
- gravar forma de acesso social definitiva;
- gravar avatar definitivo;
- gravar contexto social pendente em tabela do `eickrono-autenticacao-servidor`
  ou do `eickrono-identidade-servidor`;
- criar pessoa incompleta;
- criar cadastro finalizado sem senha.

O app pode manter os dados sociais temporarios em memoria/estado local ate que
uma decisao final aconteca:

- abrir cadastro;
- entrar e vincular;
- cancelar;
- fechar app;
- exceder tentativas locais.

### 2. Dados sociais temporarios no app nao usam token

Nao deve existir token social temporario entre o app Thimisu e o
`eickrono-autenticacao-servidor` antes do login.

Regra aprovada:

- cadastro comum nao envia token social temporario;
- cadastro iniciado por rede social tambem nao envia token social temporario;
- o app pode usar dados sociais apenas como apoio visual/prefill durante o
  fluxo em andamento;
- o app nao deve receber JWT de autenticacao antes do login concluido;
- no app, token de sessao so nasce depois de login/cadastro efetivado;
- comunicacao segura entre `eickrono-autenticacao-servidor` e
  `eickrono-identidade-servidor` pode usar JWT interno servidor-servidor;
- persistencia definitiva de rede social e avatar acontece somente dentro do
  fluxo final dos servidores.

Consequencia:

- o plano nao deve criar contrato opcional de `tokenSocialTemporario`;
- o plano nao deve exigir reautenticacao social no submit final do cadastro;
- o plano nao deve gravar pendencia social em banco;
- o app nao deve transformar dados sociais temporarios em conta local recente,
  sessao ou cache definitivo.

### 3. Persistencia definitiva da rede social

A rede social so pode ser salva quando ja existe pessoa/usuario definitivo.

Fluxos que podem salvar a rede social:

- finalizacao de cadastro;
- login local bem-sucedido no fluxo `entrar e vincular`;
- vinculacao social em area autenticada da conta, se existir fluxo futuro.

Ao salvar:

- `identidade.pessoas` ja deve existir;
- `autenticacao.usuarios` ja deve existir;
- `autenticacao.usuarios_clientes_ecossistema` ja deve existir para o projeto;
- a forma social entra em `autenticacao.usuarios_formas_acesso`;
- a foto da rede social entra em `identidade.avatar_usuario`;
- a URL da rede social nao deve ficar dentro da forma de acesso como fonte
  canonica de avatar.

### 4. Avatar preferido

Todas as opcoes de imagem do usuario dentro do projeto ficam em:

- `identidade.avatar_usuario`.

Essa tabela deve guardar:

- origem catalogada da imagem, apontando para `identidade.avatar_origens`;
- URL publica/controlada para o app carregar;
- dados tecnicos de arquivo quando a imagem foi enviada por produto Eickrono;
- versao/hash/atualizacao para cache local;
- booleano `preferido`.

Regra:

- somente `identidade.avatar_usuario.preferido` decide qual foto aparece como
  avatar do usuario no projeto;
- forma de acesso nao decide avatar principal;
- rede social nao vira "principal" porque foi usada no login;
- se nenhuma opcao estiver `preferido = true`, o app usa sigla/iniciais.

### 5. Foto carregada pelo dispositivo

Se o usuario carregou/tirou uma foto no dispositivo durante o cadastro:

- essa imagem deve ser enviada ao servidor responsavel pelo cadastro/avatar
  junto com os dados finais do cadastro;
- ela deve gerar uma URL publica/controlada;
- ela deve virar uma opcao em `identidade.avatar_usuario`;
- a origem deve apontar para o catalogo `identidade.avatar_origens`, por exemplo
  o registro de codigo `THIMISU`;
- se ela foi escolhida visualmente como preferida, deve ficar
  `preferido = true`;
- se outra foto foi escolhida como preferida, ela deve ficar
  `preferido = false`, mas ainda disponivel como opcao de avatar.

### 6. E-mail e telefone

E-mail e telefone devem ter fonte canonica unica.

Alvo:

- `identidade.contatos_email` para e-mails;
- `identidade.contatos_telefone` para telefones.

O DBML alvo inclui `identidade.contatos_telefone`.

Essa tabela deve ser usada para:

- validacao de telefone;
- contato principal;
- recuperacao ou politicas futuras;
- evitar duplicacao de telefone em cadastro, pessoa e usuario.

### 7. Tentativas no fluxo entrar e vincular

As 3 tentativas de senha pertencem ao fluxo do app.

Regra:

- o app conta as tentativas do fluxo `entrar e vincular`;
- ao cancelar ou atingir o limite, o app apaga os dados sociais temporarios;
- o `eickrono-autenticacao-servidor` pode ter protecoes de seguranca normais;
- o `eickrono-identidade-servidor` nao deve ser dono dessa regra de tentativa.

## Projetos impactados

### eickrono-autenticacao-servidor

### Responsabilidade alvo

O `eickrono-autenticacao-servidor` deve:

- validar credencial social recebida do app;
- devolver resultado funcional do fluxo;
- nao emitir token social temporario para o app antes do login;
- usar autenticacao interna servidor-servidor quando chamar o
  `eickrono-identidade-servidor`;
- nao criar usuario no Keycloak durante pre-cadastro social;
- nao gravar contexto social pendente em banco;
- permitir persistencia temporaria somente quando ja existe `cadastro_id`,
  vinculada ao proprio cadastro e consumida na confirmacao de e-mail;
- nao gravar rede social definitiva antes de existir usuario/pessoa.

### Alteracoes planejadas

1. Separar validacao social de criacao/vinculo social.

   O metodo atual que autentica com a rede social nao deve usar um caminho que
   crie usuario federado automaticamente quando o usuario ainda nao existe.

   Implementacao alvo no endpoint publico `/api/publica/sessoes/sociais`:

   - validar a credencial nativa antes de qualquer chamada ao Keycloak;
   - Google: validar o `access_token` no endpoint `userinfo` do Google;
   - Apple: validar o `identity_token` com JWKS da Apple, issuer e audience
     configurados por ambiente;
   - localizar primeiro se ja existe forma social definitiva em
     `autenticacao.usuarios_formas_acesso` para o projeto;
   - chamar token exchange do Keycloak somente quando esse vinculo definitivo
     ja existe;
   - se o vinculo definitivo nao existe, devolver erro funcional
     `social_sem_conta_local` com dados sociais validados, sem token exchange e
     sem gravar usuario/contexto pendente.

2. Criar/ajustar contrato de resposta social temporaria.

   Resposta esperada para social sem conta local:

   - codigo funcional: `social_sem_conta_local`;
   - acao sugerida: `ABRIR_CADASTRO` ou `ENTRAR_E_VINCULAR`;
   - dados sociais validados;
   - tempo de expiracao.

   Essa resposta nao deve incluir JWT/token para o app. O app so recebe token
   depois de login/cadastro efetivado.

3. Remover dependencia de tabela de contexto social pendente.

   Nenhum novo fluxo deve depender de tabela de pendencia social em servidor.
   Isso nao impede uma tabela transitoria vinculada a `cadastro_id`, porque
   nesse caso o cadastro ja existe, o dado nao autentica e so pode ser consumido
   pelo proprio processo de cadastro.

4. Definir autenticacao interna com o `eickrono-identidade-servidor`.

   Quando o `eickrono-autenticacao-servidor` precisar persistir cadastro,
   vinculo social ou avatar no `eickrono-identidade-servidor`, a chamada deve
   usar credencial interna servidor-servidor. Essa credencial pode ser JWT
   interno, mas nao e enviada ao app.

5. Atualizar documentacao antiga.

   Documentos que precisam ser revisados:

   - `documentacao/especificacao_avatar_social_e_avatar_preferido_multiapp.md`;
   - `documentacao/plano-vinculos-sociais-keycloak.md`;
   - `documentacao/guia_fluxos_login_autenticacao_app.md`, se ainda descrever
     persistencia de pendencia social no `eickrono-autenticacao-servidor` ou no
     `eickrono-identidade-servidor`.

### Testes obrigatorios

Unitarios:

- validar credencial Google/Apple sem criar usuario;
- garantir que a resposta social temporaria nao inclui JWT/token para o app;
- garantir que `/sessoes/sociais` nao chama token exchange quando nao existe
  vinculo social definitivo;
- garantir que `/sessoes/sociais` chama token exchange apenas quando o vinculo
  social definitivo existe;
- garantir que a localizacao do vinculo social definitivo filtra projeto,
  provedor, identificador externo, vinculo ativo e `sub_remoto` presente;
- validar credencial interna servidor-servidor para chamadas ao
  `eickrono-identidade-servidor`;
- diferenciar `ABRIR_CADASTRO` de `ENTRAR_E_VINCULAR`;
- manter `vinculo_social_pertence_a_outra_conta` apenas quando a rede pertence
  a outro usuario definitivo.

Integracao:

- chamada social sem conta local nao cria usuario no Keycloak;
- chamada social sem conta local nao cria registro em tabelas de usuario;
- chamada social sem conta local nao cria avatar definitivo;
- finalizacao de cadastro cria usuario, forma social e avatar sem token social
  temporario no app;
- entrar e vincular com login local valido cria forma social e avatar;
- chamada interna sem credencial servidor-servidor valida nao persiste dados.

### eickrono-identidade-servidor

### Responsabilidade alvo

O `eickrono-identidade-servidor` deve:

- manter a fonte canonica de pessoa;
- manter e-mail e telefone canonicos;
- persistir avatar definitivo do usuario no projeto;
- devolver avatar preferido na sessao;
- concluir cadastro criando pessoa, usuario, contatos, formas de acesso e
  avatars definitivos;
- nao guardar estado temporario de autenticacao social antes de existir
  pessoa/usuario final.

### Alteracoes planejadas

1. Manter DBML alvo alinhado com as decisoes fechadas.

   `avatar_perfil_schema_alvo.dbml` deve conter:

   - `identidade.contatos_telefone`;
   - relacao de `autenticacao.cadastros_conta.telefone_id` com telefone
     canonico;
   - `identidade.avatar_origens`;
   - relacao de `identidade.avatar_usuario.origem_id` e
     `autenticacao.cadastros_conta_avatares.origem_id` com o catalogo;
   - comentarios explicando que telefone nao pertence a avatar nem forma de
     acesso.

2. Criar migrations de modelo alvo.

   Tabelas alvo:

   - `identidade.pessoas`;
   - `identidade.contatos_email`;
   - `identidade.contatos_telefone`;
   - `identidade.avatar_origens`;
   - `identidade.avatar_usuario`;
   - `autenticacao.cadastros_conta_avatares`, se o cadastro ainda precisar
     guardar opcoes antes da conclusao.

3. Criar backfill do modelo atual.

   Backfill esperado:

   - pessoas legadas para `identidade.pessoas`;
   - e-mails legados para `identidade.contatos_email`;
   - telefones legados para `identidade.contatos_telefone`;
   - avatar social/preferido atual para `identidade.avatar_usuario`;
   - manter compatibilidade enquanto app e servidores ainda leem campos antigos.

4. Corrigir persistencia final do cadastro.

   Ao concluir cadastro:

   - criar pessoa definitiva;
   - criar usuario definitivo;
   - criar contato de e-mail;
   - criar contato de telefone quando aplicavel;
   - salvar todas as redes sociais confirmadas;
   - salvar todas as opcoes de avatar confirmadas;
   - marcar apenas uma opcao como preferida, ou nenhuma.

5. Corrigir resposta de sessao.

   A sessao deve trazer:

   - `usuario`;
   - `emailPrincipal`;
   - `avatarPreferidoUrl`;
   - `avatarPreferidoOrigem`;
   - `avatarPreferidoVersao` ou `avatarPreferidoAtualizadoEm`;
   - opcionalmente `avatarPreferidoHash`.

6. Remover avatar de formas de acesso como fonte canonica.

   A forma de acesso pode continuar apontando para provedor e identificador,
   mas a URL de avatar deve ser resolvida por `identidade.avatar_usuario`.

7. Planejar remocao do legado.

   Campos/tabelas legadas so devem ser removidos depois de:

   - backfill validado;
   - app lendo novo contrato;
   - servidores usando novo modelo;
   - HML validado.

### Testes obrigatorios

Unitarios:

- cadastro final cria pessoa, usuario, e-mail, telefone e avatar;
- cadastro com Google e Apple salva as duas formas sociais;
- escolha de avatar preferido nao remove outra rede social;
- foto do dispositivo gera avatar com origem catalogada de codigo `THIMISU`;
- zero avatar preferido gera sessao sem URL de avatar;
- exatamente um avatar preferido retorna a URL correta.

Integracao:

- finalizacao de cadastro com foto local salva arquivo e URL;
- finalizacao de cadastro com redes sociais salva todas as opcoes de avatar;
- login por senha retorna avatar preferido;
- login social retorna avatar preferido da conta, nao necessariamente a foto da
  rede usada no login;
- refresh/sessao silenciosa retorna avatar preferido;
- telefone validado aparece em `identidade.contatos_telefone`;
- migracao/backfill nao duplica e-mail, telefone ou avatar.

Testes de banco:

- indice unico parcial permite no maximo um avatar preferido ativo por
  `usuario_cliente_id`;
- forma social unica por `provedor + identificador_externo`;
- e-mail normalizado unico;
- telefone normalizado unico conforme regra definida;
- nenhum avatar ativo fica sem `url_avatar`.

### eickrono-thimisu-app

### Responsabilidade alvo

O app Thimisu deve:

- iniciar autenticacao social nativa;
- receber dados sociais validados do `eickrono-autenticacao-servidor`;
- manter esses dados apenas como estado temporario ate decisao final;
- nao receber nem enviar token social temporario antes do login;
- apagar dados temporarios quando o usuario cancelar, falhar limite ou fechar o
  app;
- enviar foto carregada/tirada no dispositivo no cadastro;
- receber avatar preferido na sessao;
- atualizar cache local do avatar por versao/hash/data;
- exibir sigla quando nao houver avatar preferido.

### Alteracoes planejadas

1. Revisar estado temporario social.

   O app deve guardar temporariamente:

   - provedor;
   - identificador externo mascarado/seguro para UI;
   - e-mail social;
   - nome social;
   - `url_avatar`;
   - expiracao.

   Esse estado nao deve virar conta local recente nem cache definitivo.

2. Fluxo `ABRIR_CADASTRO`.

   Ao receber `social_sem_conta_local + ABRIR_CADASTRO`:

   - mostrar mensagem inferior com `Sim, abrir cadastro` e `Agora nao`;
   - `Sim` abre cadastro com dados sociais temporarios;
   - cadastro finaliza sem token social temporario;
   - `Agora nao` limpa dados temporarios.

3. Fluxo `ENTRAR_E_VINCULAR`.

   Ao receber `social_sem_conta_local + ENTRAR_E_VINCULAR`:

   - mostrar mensagem inferior com `Entrar e vincular` e `Agora nao`;
   - app pede login por usuario/senha;
   - app controla ate 3 falhas nesse fluxo;
   - sucesso conclui o fluxo sem token social temporario no app;
   - cancelar, 3 falhas ou fechamento do app apagam dados temporarios.

4. Cadastro com foto do dispositivo.

   Se usuario carregou/tirou foto:

   - app envia imagem no fluxo final de cadastro ou usa endpoint de upload
     definido;
   - a imagem enviada deve ter origem `THIMISU`;
   - se escolhida como preferida, o payload deve indicar preferencia visual;
   - se nao for preferida, ainda pode ser enviada como opcao de avatar.

5. Cache local de avatar.

   Ao autenticar com sucesso:

   - se `avatarPreferidoUrl` vier nulo, usar sigla;
   - se `avatarPreferidoVersao` mudou, atualizar cache local;
   - se a versao for igual, manter cache;
   - se nao houver versao por compatibilidade antiga, comparar URL como fallback.

6. Sessao e contas recentes.

   A conta local recente deve usar:

   - `usuario` como login exibido/selecionado;
   - `emailPrincipal` ou e-mail mascarado como dado secundario;
   - avatar preferido da sessao, quando existir.

### Testes obrigatorios

Unitarios:

- social temporario nao cria conta local recente;
- cancelar `ABRIR_CADASTRO` limpa dados sociais temporarios;
- cancelar `ENTRAR_E_VINCULAR` limpa dados sociais temporarios;
- 3 falhas no `ENTRAR_E_VINCULAR` limpam dados sociais temporarios;
- cadastro nao envia token social temporario;
- cache de avatar atualiza quando versao muda;
- cache de avatar nao atualiza quando versao e igual;
- sessao sem avatar usa sigla.

Widget tests:

- mensagem inferior de `ABRIR_CADASTRO` mostra botoes corretos;
- mensagem inferior de `ENTRAR_E_VINCULAR` mostra botoes corretos;
- cadastro aberto por social recebe dados temporarios;
- foto local selecionada aparece como opcao de avatar;
- avatar preferido aparece apos login por senha;
- avatar preferido aparece apos login social;
- sigla aparece quando nao existe avatar.

Integration tests:

- cadastro novo com Google/Apple e foto local;
- cadastro novo com foto social preferida;
- cadastro novo com foto local preferida;
- login por senha retorna avatar preferido;
- login social retorna avatar preferido;
- entrar e vincular rede social a conta existente;
- cancelamento limpa estado temporario.

Golden tests:

- login com avatar;
- login sem avatar;
- cadastro com avatar social;
- cadastro com avatar local;
- seletor de avatar preferido;
- mensagem inferior de abrir cadastro;
- mensagem inferior de entrar e vincular.

## Cenarios funcionais para TDD

Esta matriz e a base para escrever os testes antes da implementacao. Ela cobre
os caminhos afetados pela mudanca de avatar, rede social, storage e sessao.

### Grupo A - autenticacao social inicial sem persistencia

| ID | Cenario | Entrada | Resultado esperado | Testes |
| --- | --- | --- | --- | --- |
| A1 | Rede social ja vinculada ao usuario correto | Apple/Google valido, forma social ja existe para o usuario do projeto | `eickrono-autenticacao-servidor` devolve sessao pronta; nenhum cadastro novo; nenhum aviso de vinculacao | unitario autenticacao, integracao login social, app widget entra no app |
| A2 | Rede social valida sem usuario local e sem e-mail ja cadastrado | Apple/Google valido, nao existe usuario com esse e-mail no projeto | resposta `social_sem_conta_local` + `ABRIR_CADASTRO`; servidor nao cria usuario, pessoa, forma social, avatar nem contexto pendente em banco | unitario autenticacao, integracao banco vazio, widget aviso inferior |
| A3 | Rede social valida com e-mail de usuario local existente, mas sem vinculo social | Apple/Google valido, e-mail ja pertence a usuario local do projeto | resposta `social_sem_conta_local` + `ENTRAR_E_VINCULAR`; servidor nao cria forma social antes do login local | unitario autenticacao, integracao sem persistencia, widget aviso inferior |
| A4 | Rede social ja vinculada a outro usuario | Apple/Google valido, identificador externo ja pertence a outro usuario local | resposta `vinculo_social_pertence_a_outra_conta`; app mostra conflito; nao abre cadastro nem vincula automaticamente | unitario autenticacao, integracao conflito, widget mensagem |
| A5 | Conta existe, mas nao esta liberada | rede social pertence a conta com validacao pendente | resposta `conta_nao_liberada`; app mostra aviso inferior com decisao para retomar validacao | unitario autenticacao, widget retomada |
| A6 | Conta desabilitada | rede social pertence a conta bloqueada/desabilitada | resposta `conta_desabilitada`; app abre tela de excecao de usuario bloqueado | unitario autenticacao, widget navegacao |
| A7 | Credencial social invalida | token/credencial Apple/Google invalida ou expirada | resposta `autenticacao_social_invalida`; app mostra mensagem inferior temporaria; nada e persistido | unitario autenticacao, widget mensagem temporaria |
| A8 | Falha de rede | app nao consegue chamar o servidor ou servidor indisponivel | app mostra mensagem inferior temporaria de falha de rede; estado social temporario nao e criado | app unitario/controlador, widget mensagem |
| A9 | Erro inesperado | `statusCode >= 500` ou resposta fora do contrato | app mostra mensagem generica temporaria e registra observabilidade; nada e persistido | app unitario tradutor, widget mensagem |

### Grupo B - estado temporario social no app

| ID | Cenario | Entrada | Resultado esperado | Testes |
| --- | --- | --- | --- | --- |
| B1 | `ABRIR_CADASTRO` confirmado | resposta A2 e operador toca `Sim, abrir cadastro` | app abre `/cadastro` com dados sociais temporarios para prefill; nao existe token social temporario | widget login, integration app |
| B2 | `ABRIR_CADASTRO` cancelado | resposta A2 e operador toca `Agora nao` | app fecha aviso e apaga dados sociais temporarios | unitario controlador, widget login |
| B3 | `ENTRAR_E_VINCULAR` confirmado | resposta A3 e operador toca `Entrar e vincular` | app permanece no login, preenche login sugerido quando houver, limpa senha e aguarda login local | unitario controlador, widget login |
| B4 | `ENTRAR_E_VINCULAR` cancelado | resposta A3 e operador toca `Agora nao` | app fecha aviso e apaga dados sociais temporarios | unitario controlador, widget login |
| B5 | `ENTRAR_E_VINCULAR` falha 3 vezes | resposta A3 e tres tentativas de senha falham | app cancela vinculacao pendente local e apaga dados sociais temporarios; servidor nao cria forma social | unitario controlador, integracao autenticacao |
| B6 | App fechado com dados sociais temporarios | app reinicia antes de concluir cadastro/vinculo | dados sociais temporarios nao devem reabrir fluxo nem criar conta recente | unitario persistencia local, integration app |
| B7 | Nova tentativa social substitui tentativa anterior | operador cancela ou inicia outro provedor | estado temporario anterior e descartado antes de armazenar o novo | unitario controlador |

### Grupo C - finalizacao de cadastro

| ID | Cenario | Entrada | Resultado esperado | Testes |
| --- | --- | --- | --- | --- |
| C1 | Cadastro comum sem rede social e sem foto local | payload de cadastro por usuario/senha | cria pessoa, usuario, e-mail, telefone quando houver; nao cria avatar; sessao retorna sem `avatarPreferidoUrl`; app usa sigla | unitario identidade, integracao cadastro, widget/golden sigla |
| C2 | Cadastro comum com foto local | payload de cadastro + imagem carregada/tirada no dispositivo | `eickrono-autenticacao-servidor` recebe imagem, repassa ao `eickrono-identidade-servidor`; imagem vai para S3; cria avatar origem `THIMISU` | unitario upload, integracao storage stub, banco |
| C3 | Cadastro iniciado por social, avatar social preferido | dados sociais temporarios no app + cadastro final | servidores criam pessoa/usuario, forma social e avatar origem `GOOGLE` ou `APPLE`; somente o avatar social escolhido fica `preferido = true`; todas as demais opcoes de avatar do mesmo usuario/projeto ficam `preferido = false` | unitario autenticacao/identidade, integracao cadastro social, golden |
| C4 | Cadastro iniciado por social, foto local preferida | dados sociais temporarios + imagem local preferida | cria forma social; cria avatar social `preferido = false`; cria avatar `THIMISU` `preferido = true`; upload usa bucket do ambiente | unitario identidade, integracao storage, golden |
| C5 | Cadastro com Google e Apple antes de finalizar | app agregou duas redes sociais no mesmo cadastro | salva as duas formas sociais e as duas opcoes de avatar quando houver URL; apenas uma opcao pode ficar `preferido = true` | unitario plural social, integracao cadastro |
| C6 | Cadastro com rede social sem foto | dados sociais validos sem `urlAvatarExterno` | cria forma social; nao cria avatar social sem URL; se nao houver outra foto preferida, app usa sigla | unitario identidade, integracao cadastro, golden sigla |
| C7 | Cadastro com foto local enviada, mas nao preferida | app carregou foto do dispositivo e escolheu outra origem | upload ocorre mesmo sem preferencia; cria avatar `THIMISU` `preferido = false`; avatar preferido continua sendo a origem escolhida | unitario identidade, integracao storage |
| C8 | Falha no upload da foto local | imagem local informada, storage indisponivel ou arquivo invalido | cadastro nao deve concluir como se a foto tivesse sido salva; resposta deve ser erro funcional/tecnico tratavel | unitario upload, integracao rollback |

### Grupo D - entrar e vincular

| ID | Cenario | Entrada | Resultado esperado | Testes |
| --- | --- | --- | --- | --- |
| D1 | Entrar e vincular com senha correta | dados sociais temporarios + login local valido | cria forma social para o usuario autenticado; cria avatar social se houver URL; nao troca avatar preferido sem decisao explicita | unitario autenticacao, integracao vinculo |
| D2 | Entrar e vincular com login de outro usuario | dados sociais temporarios + login local valido de usuario diferente do sugerido | servidor rejeita ou app cancela fluxo conforme regra; nao cria vinculo indevido | unitario autenticacao, app controlador |
| D3 | Entrar e vincular com social ja pertencente a outra conta | tentativa de vincular identificador externo ja usado | resposta `vinculo_social_pertence_a_outra_conta`; nada e alterado | unitario autenticacao, integracao |
| D4 | Entrar e vincular sem URL de avatar social | social valido sem foto | cria forma social; nao cria avatar social; avatar preferido existente permanece | unitario identidade/autenticacao |

### Grupo E - login, sessao e cache de avatar

| ID | Cenario | Entrada | Resultado esperado | Testes |
| --- | --- | --- | --- | --- |
| E1 | Login por senha com avatar preferido | usuario tem `identidade.avatar_usuario.preferido = true` | sessao publica retorna `avatarPreferidoUrl`, origem, versao e/ou data; app salva cache local | unitario sessao, integracao login, app unitario |
| E2 | Login social com avatar preferido diferente da rede usada | usuario entra com Google, mas avatar preferido e `THIMISU` ou `APPLE` | sessao retorna o avatar preferido da conta, nao a foto da rede usada no login | unitario sessao, integracao login social |
| E3 | Refresh/sessao silenciosa com avatar | refresh valido | resposta retorna os mesmos campos de avatar preferido da sessao normal | unitario refresh, integracao |
| E4 | Versao do avatar nao mudou | app tem avatar cacheado e servidor retorna mesma versao | app mantem arquivo local; nao baixa novamente | app unitario cache |
| E5 | Versao do avatar mudou | servidor retorna nova `avatarPreferidoVersao` | app atualiza cache local e passa a exibir a nova imagem | app unitario cache, golden |
| E6 | Servidor nao retorna versao por compatibilidade antiga | resposta antiga com URL | app usa `url_avatar` como fallback de comparacao | app unitario compatibilidade |
| E7 | Avatar preferido removido ou nulo | sessao sem `avatarPreferidoUrl` e com declaracao explicita `avatarPreferidoAusente = true` | app limpa avatar local daquele usuario quando aplicavel e usa sigla; `picture` legado do token nao pode manter avatar antigo | app unitario cache, golden |

### Grupo F - storage S3 e ambiente

| ID | Cenario | Entrada | Resultado esperado | Testes |
| --- | --- | --- | --- | --- |
| F1 | HML salva foto local | cadastro com imagem em HML | backend usa bucket `eickrono-avatares-hml`; `storage_key` relativo; app recebe apenas `url_avatar` | unitario configuracao, integracao com stub S3 |
| F2 | Producao salva foto local | cadastro com imagem em producao | backend usa bucket `eickrono-avatares-prod`; `storage_key` relativo; app nao conhece nome do bucket | unitario configuracao |
| F3 | Content type invalido | upload com arquivo nao permitido | servidor rejeita antes de persistir avatar | unitario validacao |
| F4 | Arquivo acima do limite | upload excede tamanho permitido | servidor rejeita antes de persistir avatar | unitario validacao |
| F5 | URL publica/controlada gerada | upload aceito | `url_avatar` e gerada pela camada backend/CDN e salva em `identidade.avatar_usuario` | unitario storage, integracao |

### Grupo G - migrations, legado e backfill

| ID | Cenario | Entrada | Resultado esperado | Testes |
| --- | --- | --- | --- | --- |
| G1 | Backfill de avatar social legado | dados antigos com `url_avatar_externo` | cria registros em `identidade.avatar_usuario` com origem catalogada correta | migration test |
| G2 | Backfill de avatar preferido legado | campos antigos de preferencia visual | exatamente um registro ativo fica `preferido = true` por usuario/projeto | migration test, unicidade |
| G3 | Backfill de telefone | telefone antigo em cadastro/pessoa/usuario | cria `identidade.contatos_telefone` sem duplicar telefone normalizado | migration test |
| G4 | Compatibilidade de um release | campos antigos ainda existem | leitura nova e antiga convivem ate HML validar app e servidores | teste integracao compatibilidade |
| G5 | Remocao posterior do legado | release seguinte depois de HML/prod validado | remove campos/tabelas antigas sem perder avatar, telefone ou vinculos sociais | migration test, smoke |

## Etapas de execucao

Regra obrigatoria para todas as etapas:

- antes de editar codigo ou migration, reler este documento, o DBML alvo e os
  documentos de fluxo social alinhados;
- antes de concluir a etapa, comparar o `git diff` com a documentacao e remover
  qualquer alteracao fora do escopo;
- se o codigo implementado exigir comportamento diferente do documentado, parar
  e atualizar a documentacao antes de continuar;
- nenhum ajuste oportunista deve ser feito junto com a etapa;
- cada etapa so pode avancar depois dos testes definidos para ela e de
  `git diff --check`.

Documentos que devem ser consultados em toda etapa:

- `eickrono-identidade-servidor/documentacao/plano_migracao_avatar_usuario_autenticacao_identidade_thimisu.md`;
- `eickrono-identidade-servidor/documentacao/avatar_perfil_schema_alvo.dbml`;
- `eickrono-autenticacao-servidor/documentacao/fluxograma_login_social_app.md`;
- `eickrono-autenticacao-servidor/documentacao/guia_fluxos_login_autenticacao_app.md`.

### Etapa 1 - Alinhar documentacao e DBML

Arquivos esperados:

- `eickrono-identidade-servidor/documentacao/avatar_perfil_schema_alvo.dbml`;
- este documento;
- documentos antigos de avatar/social que serao marcados como historicos ou
  atualizados.

Saida esperada:

- DBML alvo completo, incluindo telefone;
- documentos conflitantes identificados;
- nenhuma migration criada ainda.

Testes:

- revisao manual do DBML;
- `git diff --check`.

Passo a passo:

1. Abrir o DBML alvo e confirmar as tabelas canonicas:
   `identidade.contatos_email`, `identidade.contatos_telefone`,
   `identidade.avatar_origens`, `identidade.avatar_usuario`,
   `autenticacao.usuarios_formas_acesso` e
   `autenticacao.cadastros_conta_avatares`.
2. Confirmar que o DBML alvo nao inclui tabela de pendencia social persistida.
3. Confirmar que `avatar_usuario.preferido` e o unico marcador visual de avatar
   principal no projeto.
4. Confirmar que `storage_key` aponta para bucket dedicado por ambiente:
   `eickrono-avatares-hml` ou `eickrono-avatares-prod`.
5. Comparar a documentacao de login social com a regra de dados sociais
   temporarios no app.
6. Marcar documentos antigos como alinhados, historicos ou pendentes de revisao.
7. Rodar `git diff --check`.

Criterio para avancar:

- DBML e documentos explicam a mesma arquitetura;
- nao existe contradicao dizendo que o servidor deve persistir contexto social
  antes do cadastro/vinculo final.

### Etapa 2 - Definir contratos

Contratos a fechar:

- resposta de validacao social temporaria;
- payload final de cadastro;
- payload final de entrar e vincular;
- autenticacao interna entre `eickrono-autenticacao-servidor` e
  `eickrono-identidade-servidor`;
- campos de avatar na sessao.

Saida esperada:

- DTOs planejados ou criados em testes;
- nomes estaveis para campos;
- compatibilidade com app Thimisu.

Testes:

- unitarios de serializacao/deserializacao;
- unitarios da autenticacao interna servidor-servidor;
- contrato invalido rejeitado.

Passo a passo:

1. Definir DTO de resposta de validacao social pendente sem identificador de
   contexto social persistido, sem token social temporario e sem JWT de sessao.
2. Definir DTO do payload final de cadastro com lista plural de redes sociais
   confirmadas e opcoes de avatar.
3. Definir DTO do payload final de entrar-e-vincular usando dados sociais
   confirmados no fluxo e login local valido.
4. Definir campos de avatar retornados na sessao:
   `avatarPreferidoUrl`, `avatarPreferidoOrigem`,
   `avatarPreferidoVersao`, `avatarPreferidoAtualizadoEm` e fallback por URL.
5. Definir contrato de upload de foto local pelo caminho:
   app Thimisu -> `eickrono-autenticacao-servidor` ->
   `eickrono-identidade-servidor`.
6. Definir contrato interno servidor-servidor com JWT interno entre
   `eickrono-autenticacao-servidor` e `eickrono-identidade-servidor`.
7. Criar testes de serializacao antes da implementacao real.
8. Comparar nomes dos campos com os cenarios C, D, E e F da matriz de TDD.

Criterio para avancar:

- contratos cobrem todos os cenarios A-G;
- nenhum contrato exige token social temporario no app;
- nenhum contrato exige reautenticacao social no submit final do cadastro.

### Etapa 3 - Migrations e backfill no eickrono-identidade-servidor

Status atual: iniciada. A migration `V38__criar_modelo_canonico_avatar_usuario.sql`
cria o primeiro modelo canonico de pessoa, contatos e avatar, incluindo
catalogo `identidade.avatar_origens`, `identidade.avatar_usuario`,
`identidade.contatos_email`, `identidade.contatos_telefone` e
`autenticacao.cadastros_conta_avatares`. Ela tambem faz backfill inicial a
partir do legado sem remover campos antigos.

Validacao executada:

- `EICKRONO_TEST_PREFER_LOCAL_POSTGRES=true mvn -Dtest=AvatarUsuarioModeloCanonicoMigrationTest test`
  executado contra PostgreSQL local em `localhost:5432`;
- resultado apos incluir validacao de origens e unicidade real: 7 testes,
  0 falhas, 0 erros;
- a primeira execucao identificou referencia incorreta a
  `autenticacao.cadastros_conta.telefone_confirmado_em`;
- a migration foi corrigida para usar `cadastros_conta.telefone_confirmado_em`,
  que e a coluna real do legado.
- `EICKRONO_TEST_PREFER_LOCAL_POSTGRES=true mvn -Dtest=VinculosSociaisControllerIT test`
  validou o fluxo de vinculo social que perde foto do provedor e passa a
  devolver `avatarPreferidoOrigem = NENHUM` sem recriar preferencia canonica;
- `EICKRONO_TEST_PREFER_LOCAL_POSTGRES=true mvn -Dtest=AvatarUsuarioModeloCanonicoMigrationTest,VinculoSocialServiceTest,VinculosSociaisControllerIT test`
  executado contra PostgreSQL local;
- resultado da suite focada apos os testes adicionais de unicidade: 38 testes,
  0 falhas, 0 erros.

Codigo operacional iniciado:

- `AvatarSocialProjetoJdbc` passou a criar/atualizar opcoes de avatar social em
  `identidade.avatar_usuario`;
- a escolha de avatar preferido em fluxo social passou a alterar
  `identidade.avatar_usuario.preferido`;
- quando uma rede social perde a foto, a preferencia canonica e removida e o
  contrato publico continua retornando `NENHUM`;
- o `eickrono-autenticacao-servidor` tambem recebeu a migration
  `V30__criar_modelo_canonico_avatar_usuario.sql`, com as tabelas
  `identidade.avatar_origens`, `identidade.avatar_usuario`,
  `autenticacao.cadastros_conta_avatares`, seeds de origem e backfill inicial
  dos campos legados de avatar social/preferido;
- o `AvatarSocialProjetoJdbc` do `eickrono-autenticacao-servidor` passou a ler
  a preferencia de avatar diretamente de `identidade.avatar_usuario`, incluindo
  `versao` e `atualizado_em`, para alimentar a sessao publica;
- o `eickrono-identidade-servidor` tambem passou a devolver
  `avatarPreferidoUrl`, `avatarPreferidoOrigem`, `avatarPreferidoVersao` e
  `avatarPreferidoAtualizadoEm` nas sessoes publicas por senha e por rede
  social, lendo a preferencia canonica de `identidade.avatar_usuario`;
- os campos legados de forma de acesso ainda permanecem para compatibilidade de
  leitura e DTOs existentes.

Ainda nao concluido nesta etapa:

- testes especificos de backfill com dados legados duplicados;
- remover legado;
- revisar consumidores restantes fora de `AvatarSocialProjetoJdbc` que ainda
  possam consultar campos legados de avatar;
- implementar upload real para S3.

Saida esperada:

- tabelas canonicas criadas;
- dados atuais migrados;
- campos antigos ainda mantidos enquanto houver compatibilidade.

Testes:

- testes de migration;
- testes de repositorio;
- testes de unicidade;
- testes de backfill com dados duplicados.

Passo a passo:

1. Criar migration para `identidade.contatos_telefone`.
2. Criar migration para `identidade.avatar_origens`.
3. Criar migration para `identidade.avatar_usuario`.
4. Criar/ajustar `autenticacao.cadastros_conta_avatares` conforme o DBML alvo,
   se essa tabela continuar necessaria no fluxo de cadastro.
5. Adicionar seeds/catalogo de origem:
   `GOOGLE`, `APPLE`, `THIMISU` e futuras origens permitidas.
6. Adicionar indice unico parcial para garantir no maximo um
   `avatar_usuario.preferido = true` ativo por usuario/projeto.
7. Adicionar constraints para impedir avatar ativo sem `url_avatar`.
8. Criar backfill de avatar social legado para `identidade.avatar_usuario`.
9. Criar backfill de avatar preferido legado garantindo apenas um preferido por
   usuario/projeto.
10. Criar backfill de telefone para `identidade.contatos_telefone`.
11. Manter campos antigos pelo periodo de compatibilidade definido.
12. Rodar testes de migration e repositorio.
13. Revisar `git diff` contra o DBML alvo antes de avancar.

Criterio para avancar:

- migrations reproduzem o DBML alvo;
- backfill nao duplica e-mail, telefone ou avatar;
- banco impede dois avatars preferidos ativos para o mesmo usuario/projeto.

### Etapa 4 - Validacao social sem persistencia no eickrono-autenticacao-servidor

Status atual: implementada no caminho publico de login social e validada com
suite focada do `eickrono-autenticacao-servidor`.

Alteracoes aplicadas:

- `POST /api/publica/sessoes/sociais` valida a credencial social nativa antes de
  chamar token exchange do Keycloak;
- o servidor localiza primeiro se ja existe forma social definitiva em
  `autenticacao.usuarios_formas_acesso`;
- quando a forma social definitiva existe, o fluxo segue para token exchange e
  emite sessao;
- quando a forma social definitiva nao existe, o fluxo retorna
  `social_sem_conta_local` com `ABRIR_CADASTRO` ou `ENTRAR_E_VINCULAR`;
- nesse caminho sem conta local, a resposta nao inclui identificador de
  contexto social persistido;
- nesse caminho sem conta local, o teste garante que
  `AutenticacaoSessaoInternaServico.autenticarSocial(...)` nao e chamado.

Validacao executada:

- `mvn -pl modulos/modulo-eickrono-autenticacao -Dtest=ValidadorCredencialSocialNativaServiceTest,LocalizadorLoginSocialProjetoJdbcTest,CadastroVinculoSocialConfirmadoJdbcTest,CadastroContaInternaServicoTest,FluxoPublicoControllerTest,FluxoPublicoControllerIT,RegistroDispositivoControllerTest,RegistroDispositivoControllerIT test`
- resultado: 70 testes, 0 falhas, 0 erros.

Saida esperada:

- autenticacao social inicial valida credencial sem criar usuario;
- resposta social temporaria sem token/JWT para o app;
- Keycloak nao cria usuario nesse caminho.

Testes:

- unitarios;
- integracao com mock/stub de provedor social;
- verificacao de que tabelas definitivas continuam vazias nesse fluxo.

Passo a passo:

1. Localizar o metodo que processa `POST /api/publica/sessoes/sociais`.
2. Separar validacao da credencial social de persistencia definitiva.
3. Garantir que o caminho sem conta local nao crie usuario no Keycloak.
4. Garantir que o caminho sem conta local nao chame persistencia de pessoa,
   forma social, avatar ou contexto social pendente.
5. Classificar a resposta em:
   sessao pronta, dispositivo pendente, `ABRIR_CADASTRO`,
   `ENTRAR_E_VINCULAR`, `vinculo_social_pertence_a_outra_conta`,
   `conta_nao_liberada`, `conta_desabilitada` ou erro tratavel.
6. Escrever testes para os cenarios A1 ate A9 antes ou junto da alteracao.
7. Rodar testes unitarios e integracao com provider social mockado.
8. Revisar no diff se algum caminho voltou a criar pendencia social persistida.

Criterio para avancar:

- login social sem conta local devolve dados sociais temporarios ao app;
- tabelas definitivas continuam vazias nesse fluxo;
- Keycloak nao recebe usuario novo nesse caminho.

### Etapa 5 - Persistencia final no cadastro e entrar-e-vincular

Status atual: parcialmente implementada.

Implementado ate aqui:

- o fluxo `ENTRAR_E_VINCULAR` passou a concluir a persistencia do vinculo social
  somente depois de login local valido;
- o app nao reautentica a rede social nesse fechamento;
- o app envia os dados sociais temporarios ja validados no fluxo em andamento;
- o `eickrono-autenticacao-cliente` passou a expor chamada autenticada para
  vincular rede social confirmada sem `tokenExterno`;
- o `eickrono-autenticacao-servidor` passou a aceitar a vinculacao confirmada
  sem token externo nesse endpoint autenticado;
- `VinculoSocialService` materializa a forma social definitiva e sincroniza a
  opcao de avatar social quando houver URL;
- o fluxo continua bloqueando conflito quando o identificador social ja pertence
  a outro usuario;
- o cadastro publico do `eickrono-autenticacao-servidor` passou a aceitar
  `avatarCadastroConfirmado` e `avataresCadastroConfirmados` como metadados de
  avatares ja conhecidos pelo app no momento do cadastro;
- esses metadados sao registrados em
  `autenticacao.cadastros_conta_avatares`, sempre vinculados ao `cadastro_id`,
  sem criar avatar definitivo antes da conclusao do cadastro;
- na confirmacao do e-mail, o servidor consome os avatares do cadastro e cria as
  opcoes definitivas em `identidade.avatar_usuario`;
- a validacao do cadastro impede mais de uma preferencia visual entre avatares
  sociais e avatares locais enviados no mesmo cadastro.
- a leitura de avatar preferido usa o modelo canonico
  `identidade.avatar_usuario`; quando um schema legado de teste ainda nao tem a
  tabela canonica, o servidor volta para a leitura legada em
  `autenticacao.usuarios_clientes_ecossistema` para nao quebrar fluxos que ainda
  nao executaram a migracao.
- o cadastro publico passou a aceitar foto local do app como avatar de origem
  `THIMISU`, enviada em `avataresCadastroConfirmados` com `conteudoBase64`,
  `nomeArquivo`, `contentType`, `tamanhoBytes` e `preferido`;
- o `eickrono-autenticacao-servidor` materializa avatar local antes de gravar o
  cadastro, chamando o `eickrono-identidade-servidor` por backchannel interno;
- o `eickrono-identidade-servidor` recebeu endpoint interno de upload que valida
  tipo/tamanho/base64, gera `storage_key`, `url_avatar`, `hash_conteudo` e
  `versao`, e grava o arquivo em storage local configuravel, mantendo o adapter
  pronto para troca futura por S3.

Configuracao de storage para HML:

- `identidade-hml` deve usar adapter S3, nao storage local efemero;
- o bucket de HML e `eickrono-avatares-hml`;
- o bucket deve ficar com acesso publico direto bloqueado;
- a leitura publica/controlada continua passando pela rota do
  `eickrono-identidade-servidor`:
  `https://id-hml.eickrono.store/identidade/avatares/publicos/**`;
- por isso o app continua recebendo `url_avatar`, nunca `storage_key` nem nome
  de bucket;
- variaveis obrigatorias no task definition `identidade-hml`:
  `IDENTIDADE_AVATAR_STORAGE_TIPO=s3`;
  `IDENTIDADE_AVATAR_STORAGE_BUCKET=eickrono-avatares-hml`;
  `IDENTIDADE_AVATAR_STORAGE_REGION=sa-east-1`;
  `IDENTIDADE_AVATAR_STORAGE_PUBLIC_URL_BASE=https://id-hml.eickrono.store/identidade/avatares/publicos`;
  `IDENTIDADE_AVATAR_STORAGE_MAX_BYTES=5242880`;
- CloudFront/CDN continua sendo etapa separada. Quando for criada, somente
  `IDENTIDADE_AVATAR_STORAGE_PUBLIC_URL_BASE` deve mudar para o dominio CDN,
  mantendo o mesmo `storage_key` canonico.
- a role ECS do `identidade-hml` precisa de `s3:PutObject` e `s3:GetObject` em
  `arn:aws:s3:::eickrono-avatares-hml/*`;
- a mesma role tambem precisa de `s3:ListBucket` em
  `arn:aws:s3:::eickrono-avatares-hml`, porque sem essa permissao o S3 pode
  responder `AccessDenied` para objeto inexistente e a rota publica viraria
  HTTP 502 em vez de HTTP 404.

Validacao executada:

- `mvn -pl modulos/modulo-eickrono-autenticacao -Dtest=VinculoSocialServiceTest,VinculosSociaisControllerTest test`
  no `eickrono-autenticacao-servidor`: 19 testes, 0 falhas, 0 erros;
- suite focada do `eickrono-autenticacao-servidor` com controladores e servicos
  de fluxo publico, registro de dispositivo e vinculos sociais: 89 testes,
  0 falhas, 0 erros;
- `flutter test test/cliente_api_conta_eickrono_test.dart` no
  `eickrono-autenticacao-cliente`: 18 testes, 0 falhas;
- `flutter test` no `eickrono-autenticacao-cliente`: 71 testes, 0 falhas;
- `mvn -pl modulos/modulo-eickrono-autenticacao
  -Dtest=CadastroAvatarConfirmadoJdbcTest,CadastroContaInternaServicoTest,FluxoPublicoControllerTest,CadastroApiRequestTest
  test` no `eickrono-autenticacao-servidor`: 33 testes, 0 falhas, 0 erros.
- `mvn -pl modulos/modulo-eickrono-autenticacao
  -Dtest=AvatarSocialProjetoJdbcTest,CadastroAvatarConfirmadoJdbcTest,CadastroVinculoSocialConfirmadoJdbcTest,LocalizadorLoginSocialProjetoJdbcTest,ValidadorCredencialSocialNativaServiceTest,CadastroContaInternaServicoTest,FluxoPublicoControllerTest,FluxoPublicoControllerIT,VinculoSocialServiceTest,VinculosSociaisControllerTest,RegistroDispositivoControllerTest,RegistroDispositivoControllerIT,CadastroApiRequestTest
  test` no `eickrono-autenticacao-servidor`: 96 testes, 0 falhas, 0 erros;
- `mvn -pl modulos/modulo-eickrono-autenticacao
  -Dtest=AplicacaoApiIdentidadeTest test` no
  `eickrono-autenticacao-servidor`: 4 testes, 0 falhas, 0 erros; o Docker do
  Testcontainers nao estava disponivel no ambiente local e o teste executou com
  PostgreSQL local em `localhost:5432`.
- `mvn -pl modulos/modulo-eickrono-autenticacao
  -Dtest=UploadAvatarCadastroIdentidadeHttpTest,CadastroAvatarConfirmadoJdbcTest,CadastroContaInternaServicoTest
  test` no `eickrono-autenticacao-servidor`: 24 testes, 0 falhas, 0 erros;
- `mvn -Dtest=AvatarUsuarioUploadServiceTest,AvatarUsuarioInternoControllerTest
  test` no `eickrono-identidade-servidor`: 3 testes, 0 falhas, 0 erros.
- HML em AWS atualizado em 2026-05-21 para validacao do fluxo de avatar local:
  `identidade-hml` em `task-definition/identidade-hml:42` com imagem
  `eickrono-identidade-servidor:hml-20260521-avatar-upload-01`;
  `auth-hml` em `task-definition/auth-hml:18` com imagem
  `eickrono-autenticacao-servidor:hml-20260521-avatar-upload-01`.
- Validacao HML minima executada apos rollout: OIDC HML retornou HTTP 200,
  `https://id-hml.eickrono.store/actuator/health` retornou `UP`, a rota
  publica `/identidade/avatares/publicos/**` respondeu pelo servico novo, e a
  task definition `identidade-hml:42` contem
  `IDENTIDADE_AVATAR_STORAGE_DIRETORIO`,
  `IDENTIDADE_AVATAR_STORAGE_PUBLIC_URL_BASE` e
  `IDENTIDADE_AVATAR_STORAGE_MAX_BYTES`.
- HML em AWS atualizado em 2026-05-21 para storage S3:
  `identidade-hml` em `task-definition/identidade-hml:43` com imagem
  `eickrono-identidade-servidor:hml-20260521-avatar-s3-01`;
  buckets criados `eickrono-avatares-hml` e `eickrono-avatares-prod`, ambos com
  bloqueio de acesso publico direto, criptografia AES256 e versionamento;
  `identidade-hml:43` configurado com
  `IDENTIDADE_AVATAR_STORAGE_TIPO=s3`,
  `IDENTIDADE_AVATAR_STORAGE_BUCKET=eickrono-avatares-hml`,
  `IDENTIDADE_AVATAR_STORAGE_REGION=sa-east-1`,
  `IDENTIDADE_AVATAR_STORAGE_PUBLIC_URL_BASE=https://id-hml.eickrono.store/identidade/avatares/publicos`
  e `IDENTIDADE_AVATAR_STORAGE_MAX_BYTES=5242880`;
  `https://id-hml.eickrono.store/actuator/health` retornou `UP`; rota publica
  de avatar inexistente retornou HTTP 404 depois de incluir `s3:ListBucket` na
  role ECS de HML; um PNG minimo enviado para
  `s3://eickrono-avatares-hml/avatares/thimisu/validacao-s3-hml.png` foi lido
  pela rota publica com HTTP 200, `content-type: image/png` e `cache-control:
  public, max-age=86400`; o objeto de teste foi removido depois da validacao.

Ainda nao concluido nesta etapa:

- validar em HML o upload real da foto local com origem `THIMISU` no bucket
  `eickrono-avatares-hml`;
- criar/configurar o bucket `eickrono-avatares-prod` antes de habilitar
  producao;
- criar CloudFront/CDN quando houver dominio e certificado definidos;
- validacao no iPhone fisico dos cenarios C e D usando o app HML atualizado.

Saida esperada:

- cadastro final salva redes/avatars sem token social temporario no app;
- entrar-e-vincular salva vinculo apos login local valido, sem token social
  temporario no app;
- conflito de rede ja vinculada a outro usuario continua bloqueado.

Testes:

- cadastro novo;
- conta existente;
- rede social ja vinculada ao mesmo usuario;
- rede social vinculada a outro usuario;
- chamada interna sem credencial servidor-servidor valida e rejeitada.

Passo a passo:

1. Ajustar finalizacao de cadastro para receber lista plural de redes sociais
   confirmadas.
2. Persistir temporariamente essa lista somente vinculada ao `cadastro_id` ate a
   confirmacao de e-mail, usando
   `autenticacao.cadastros_conta_vinculos_sociais_confirmados`.
3. No cadastro final, criar pessoa, usuario, contatos, formas sociais e avatars
   em uma unidade transacional coerente.
4. Persistir todas as redes sociais confirmadas, sem depender de rede
   "principal".
5. Persistir todas as opcoes de avatar confirmadas.
6. Garantir que apenas uma opcao fique `preferido = true`; se nenhuma vier como
   preferida, nenhuma deve ser forçada.
7. Implementar foto local com origem `THIMISU` e upload no bucket do ambiente.
8. Ajustar entrar-e-vincular para persistir a forma social somente depois do
   login local valido.
9. Garantir que login local de outro usuario nao vincule indevidamente a rede.
10. Garantir que rede ja vinculada a outro usuario continue bloqueada.
11. Escrever testes para C1-C8 e D1-D4.
12. Rodar testes unitarios e integracao dos servidores.
13. Revisar o diff contra as secoes de persistencia definitiva e avatar
    preferido deste documento.

Criterio para avancar:

- cadastro e entrar-e-vincular sao os unicos caminhos que salvam rede social
  nova;
- todas as redes confirmadas sao salvas;
- preferencia visual de avatar nao remove nem escolhe rede social principal.
- qualquer registro temporario por `cadastro_id` e consumido ao confirmar o
  cadastro e nao fica disponivel para outro fluxo.

### Etapa 6 - App Thimisu

Status atual: parcialmente implementada.

Implementado ate aqui:

- `ENTRAR_E_VINCULAR` mantem os dados sociais apenas no estado temporario do app
  ate o login local concluir;
- depois do login local valido, o app chama a vinculacao social autenticada com
  os dados temporarios, sem pedir nova autenticacao Apple/Google;
- o app nao envia identificador de contexto social persistido no login local
  desse fluxo novo;
- ao concluir ou falhar a vinculacao, o controlador limpa o estado temporario
  conforme o resultado;
- a sessao publica do `eickrono-autenticacao-servidor` propaga
  `avatarPreferidoVersao` junto com URL, origem e data de atualizacao;
- o app Thimisu persiste `avatarVersao` e `avatarAtualizadoEm` no catalogo local
  de contas para comparar avatar recebido pela sessao com o cache local.
- o `eickrono-autenticacao-cliente` marca explicitamente a ausencia de avatar
  preferido com `avatarPreferidoAusente = true` quando o backend nao retorna
  `avatarPreferidoUrl`;
- o app Thimisu usa `avatarPreferidoAusente = true` como declaracao
  autoritativa para limpar avatar local, versao e data, evitando manter foto
  antiga vinda de `picture` ou de cache local.
- o `eickrono-autenticacao-cliente` passou a aceitar e enviar
  `avatarCadastroConfirmado` e `avataresCadastroConfirmados` no cadastro
  publico, mantendo esses campos ausentes quando nao houver avatar confirmado.
- o app Thimisu passou a enviar foto local carregada/tirada do dispositivo no
  fechamento do cadastro, dentro de `avataresCadastroConfirmados`, mesmo quando
  a foto social e a preferida;
- a foto local enviada pelo app usa origem `THIMISU`; se ela for a escolha
  visual do usuario, vai com `preferido = true`, senao segue como opcao
  disponivel com `preferido = false`.

Validacao executada:

- `flutter test test/funcionalidades/autenticacao/aplicacao/controlador_login_test.dart`
  no app Thimisu: 41 testes, 0 falhas;
- suite focada de conta local, controlador de login e cadastro: 70 testes,
  0 falhas;
- `mvn -pl modulos/modulo-eickrono-autenticacao
  -Dtest=FluxoPublicoControllerTest,VinculoSocialServiceTest test` no
  `eickrono-autenticacao-servidor`: 25 testes, 0 falhas;
- `flutter test
  test/funcionalidades/autenticacao/aplicacao/conta_local_dispositivo_test.dart
  test/infraestrutura/autenticacao/catalogo_local_contas_drift_test.dart` no
  app Thimisu: 28 testes, 0 falhas;
- `flutter test test/autenticador_login_publico_test.dart` no
  `eickrono-autenticacao-cliente`: 5 testes, 0 falhas;
- `flutter test
  test/funcionalidades/autenticacao/aplicacao/conta_local_dispositivo_test.dart
  test/infraestrutura/autenticacao/catalogo_local_contas_drift_test.dart` no
  app Thimisu apos o marcador de avatar ausente: 29 testes, 0 falhas;
- `flutter analyze` no `eickrono-autenticacao-cliente`: sem alertas;
- `flutter test test/cliente_fluxo_publico_autenticacao_http_test.dart` no
  `eickrono-autenticacao-cliente`: 18 testes, 0 falhas;
- `flutter test` no `eickrono-autenticacao-cliente`: 72 testes, 0 falhas;
- `flutter analyze` no app Thimisu: sem alertas;
- `git diff --check` no `eickrono-autenticacao-cliente` e no app Thimisu:
  sem problemas.
- `mvn -pl modulos/modulo-eickrono-autenticacao
  -Dtest=AvatarSocialProjetoJdbcTest,FluxoPublicoControllerTest,VinculoSocialServiceTest
  test` no `eickrono-autenticacao-servidor`: 26 testes, 0 falhas;
- `mvn -pl modulos/modulo-eickrono-autenticacao
  -Dtest=AplicacaoApiIdentidadeTest test` no `eickrono-autenticacao-servidor`:
  4 testes, 0 falhas, Flyway validou e aplicou 30 migrations em PostgreSQL
  local.
- `mvn -Dtest=FluxoPublicoControllerIT,VinculoSocialServiceTest,AvatarUsuarioModeloCanonicoMigrationTest test`
  no `eickrono-identidade-servidor`: 52 testes, 0 falhas; o Testcontainers nao
  encontrou Docker funcional e a suite executou com PostgreSQL local em
  `localhost:5432`;
- `flutter test test/cliente_fluxo_publico_autenticacao_http_test.dart test/autenticador_login_publico_test.dart test/cliente_api_conta_eickrono_test.dart`
  no `eickrono-autenticacao-cliente`: 41 testes, 0 falhas;
- `flutter test test/funcionalidades/autenticacao/aplicacao/conta_local_dispositivo_test.dart test/infraestrutura/autenticacao/catalogo_local_contas_drift_test.dart`
  no app Thimisu: 29 testes, 0 falhas;
- `flutter test test/funcionalidades/autenticacao/apresentacao/pagina_cadastro_usuario_widget_test.dart`
  no app Thimisu: 23 testes, 0 falhas;
- `flutter analyze` no `eickrono-autenticacao-cliente` e no app Thimisu:
  sem alertas;
- `git diff --check` no `eickrono-identidade-servidor`,
  `eickrono-autenticacao-cliente` e app Thimisu: sem problemas.
- `flutter test test/funcionalidades/autenticacao/apresentacao/pagina_cadastro_usuario_widget_test.dart`
  no app Thimisu apos envio de foto local do cadastro: 24 testes, 0 falhas;
- `flutter analyze
  lib/funcionalidades/autenticacao/apresentacao/pagina_cadastro_usuario.dart
  test/funcionalidades/autenticacao/apresentacao/pagina_cadastro_usuario_widget_test.dart`
  no app Thimisu: sem alertas.

Ainda nao concluido nesta etapa:

- uso final do arquivo local baixado/cacheado por `avatarPreferidoVersao` com
  fallback por URL/data;
- golden tests dos estados visuais de avatar;
- validacao em iPhone fisico HML.

Saida esperada:

- estado social temporario controlado no app;
- cadastro nao recebe token social temporario;
- entrar-e-vincular controla tentativas;
- avatar preferido atualiza cache local;
- UI mostra foto ou sigla corretamente.

Testes:

- unitarios de controladores;
- widget tests;
- integration tests;
- golden tests.

Passo a passo:

1. Revisar controladores de login social para manter dados sociais apenas em
   estado temporario.
2. Remover qualquer dependencia de identificador de contexto social persistido
   ou token social temporario.
3. Garantir que `Agora nao`, fechamento do app ou nova tentativa limpem dados
   sociais temporarios.
4. Garantir que `ABRIR_CADASTRO` abre cadastro com prefill social e avatar
   social disponivel.
5. Garantir que `ENTRAR_E_VINCULAR` permanece no login, preenche login sugerido
   quando houver e controla ate 3 falhas localmente.
6. Ajustar cadastro para enviar redes sociais confirmadas e foto local quando
   existir.
7. Ajustar cadastro para indicar exatamente uma opcao de avatar preferida, ou
   nenhuma.
8. Ajustar cache de avatar para usar `avatarPreferidoVersao` como principal e
   `url_avatar`/`atualizado_em` como fallback.
9. Garantir que conta recente so nasce depois de sessao real.
10. Escrever testes unitarios, widget tests, integration tests e golden tests
    dos cenarios B, C, D e E.
11. Rodar `flutter analyze`, testes focados, integration tests possiveis e
    golden tests.
12. Revisar o diff contra o fluxo documentado antes de instalar no iPhone.

Criterio para avancar:

- app nao cria conta local recente a partir de dado social temporario;
- app nao envia token social temporario;
- app exibe avatar ou sigla conforme sessao retornada.

### Etapa 7 - Limpeza do legado

Esta etapa nao e uma remocao unica. Existem dois tipos de legado com riscos
diferentes:

1. Legado do fluxo social pendente, que pode ser removido quando nao houver
   consumidor funcional novo.
2. Legado de pessoa/perfil/acesso numerico, que so pode ser removido depois da
   migracao `pessoaCanonicaId UUID`.

Remocoes que podem ocorrer antes da migracao `pessoaCanonicaId UUID`, desde que
os testes provem ausencia de consumidor:

- tabela de contexto social pendente;
- campos antigos de contexto social persistido;
- contratos antigos de vinculo social pendente;
- endpoints de cancelamento/consumo de contexto social pendente.

Remocoes bloqueadas ate depois da migracao `pessoaCanonicaId UUID`:

- `pessoas_identidade`;
- `perfis_identidade`;
- `pessoas_formas_acesso`;
- `vinculos_sociais`;
- colunas `pessoa_id_perfil`;
- fallbacks `ClienteContextoPessoaPerfilSistemaLegado`;
- construtores antigos que instanciam resolvedores com repositorios legados.

Remocoes de avatar que podem ocorrer em etapa propria, se o app e os servidores
ja estiverem lendo o modelo novo:

- campos de avatar em formas de acesso, quando nao houver consumidor;
- campos `avatar_preferido_*` antigos em `usuarios_clientes_ecossistema`, quando
  o contrato de sessao ja devolver avatar pelo modelo canonico;
- caches locais antigos do app, se houver migration local segura.

Testes:

- migration de remocao do contexto social pendente sem consumidores ativos;
- migration de avatar sem consumidores antigos;
- migracao `pessoaCanonicaId UUID` validada antes de remover legado de pessoa;
- rollback operacional documentado quando a remocao for estrutural;
- consultas antigas nao usadas;
- smoke de login, cadastro, social, avatar e contas recentes.

Passo a passo:

1. Confirmar que app e servidores ja usam os contratos novos do pacote que sera
   limpo.
2. Confirmar que nenhum consumidor usa contexto social pendente antes de dropar
   essa estrutura.
3. Confirmar que nenhum consumidor usa campos antigos de avatar em formas de
   acesso.
4. Confirmar que nenhum consumidor usa `avatar_preferido_*` antigo.
5. Para legado de pessoa/perfil/acesso, confirmar antes que
   `pessoaCanonicaId UUID` esta gravado, lido e validado em cadastro, login,
   social, dispositivo, biometria, avatar, recuperacao e exclusao.
6. Criar migration de remocao ou descontinuacao controlada somente para o grupo
   de legado liberado.
7. Atualizar documentacao marcando exatamente qual legado foi removido e qual
   continua bloqueado.
8. Rodar smoke completo antes de qualquer deploy produtivo.

Criterio para avancar:

- remocao nao quebra cadastro, login, social, avatar, biometria, dispositivo e
  contas recentes;
- nenhum teste novo depende de estruturas removidas;
- rollback operacional esta documentado quando a remocao for estrutural;
- legado de pessoa/perfil/acesso nao e removido antes da migracao
  `pessoaCanonicaId UUID`.

### Etapa 8 - HML e iPhone fisico

Checklist:

- deploy do `eickrono-autenticacao-servidor`;
- deploy do `eickrono-identidade-servidor`;
- app Thimisu instalado em iPhone fisico HML;
- limpar usuarios orfaos de HML antes do teste, se ainda existirem;
- testar cadastro novo com Google;
- testar cadastro novo com Apple;
- testar entrar-e-vincular;
- testar login por senha com avatar;
- testar login social com avatar;
- testar foto local de galeria;
- testar foto local de camera;
- testar refresh/sessao silenciosa;
- verificar banco depois de cada caso.

Passo a passo:

1. Antes do deploy, revisar os documentos e a matriz de cenarios.
2. Fazer deploy do `eickrono-identidade-servidor`.
3. Fazer deploy do `eickrono-autenticacao-servidor`.
4. Instalar o app Thimisu HML no iPhone fisico.
5. Executar os cenarios de HML em ordem:
   C1, C2, C3, C4, C5, D1, D3, E1, E2, E3, F1.
6. Depois de cada caso, consultar banco para confirmar:
   pessoa, usuario, contatos, formas sociais, avatars, `preferido` unico,
   `storage_key`, `url_avatar` e ausencia de contexto social pendente.
7. Conferir no app:
   avatar preferido, sigla, cache, login por senha, login social e
   refresh/sessao silenciosa.
8. Registrar falhas com cenario da matriz, log, endpoint e tabela afetada.
9. Se surgir divergencia, atualizar documentacao antes de corrigir codigo.

Criterio para concluir:

- todos os casos de HML passam no iPhone fisico;
- banco confirma o modelo alvo;
- documentos e comportamento real continuam alinhados.

## Documentos que precisam ser atualizados

### Documentos ja alinhados com esta revisao

- `eickrono-autenticacao-servidor/documentacao/fluxograma_login_social_app.md`
  foi atualizado para trocar contexto social persistido por dados sociais
  temporarios no app, sem token social temporario e sem persistencia de
  pendencia em banco.
- `eickrono-autenticacao-servidor/documentacao/guia_fluxos_login_autenticacao_app.md`
  foi atualizado nos pontos de login social, cadastro em andamento e estados
  tecnicos para refletir que a classificacao funcional acontece no
  `eickrono-autenticacao-servidor`, enquanto a persistencia definitiva so
  acontece no fechamento do cadastro ou do entrar-e-vincular.
- `eickrono-autenticacao-servidor/documentacao/especificacao_avatar_social_e_avatar_preferido_multiapp.md`
  foi marcado como historico para novas implementacoes. O texto agora aponta
  para este plano, para o DBML alvo e para os documentos de login social
  atualizados.
- `eickrono-autenticacao-servidor/documentacao/plano-vinculos-sociais-keycloak.md`
  foi marcado como historico para novas implementacoes. O documento deixa claro
  que Keycloak nao deve criar usuario/vinculo no pre-cadastro social.
- `eickrono-thimisu/eickrono-thimisu-app/docs/entendimento_foto_perfil_cadastro_login.md`
  foi atualizado para trocar `contexto social pendente` por dados sociais
  temporarios mantidos somente no app, e para apontar avatar definitivo para
  `identidade.avatar_usuario`.
- `eickrono-identidade-servidor/documentacao/avatar_perfil_schema_alvo.dbml`
  foi atualizado para diferenciar dados sociais temporarios vinculados a
  `cadastro_id` de contexto social pendente global. Essa tabela temporaria nao
  autentica e so existe para concluir o proprio cadastro.

### `eickrono-autenticacao-servidor/documentacao/especificacao_avatar_social_e_avatar_preferido_multiapp.md`

Problema:

- documento historico ainda contem secoes antigas mantidas para consulta.

Acao:

- nao usar como fonte canonica para novas implementacoes;
- se for reutilizado no futuro, reescrever completamente usando
  `identidade.avatar_usuario`.

### `eickrono-autenticacao-servidor/documentacao/plano-vinculos-sociais-keycloak.md`

Problema:

- documento historico ainda contem secoes antigas mantidas para consulta.

Acao:

- nao usar como fonte canonica para novas implementacoes;
- se for reutilizado no futuro, reescrever completamente mantendo Keycloak fora
  do pre-cadastro social.

### `eickrono-thimisu/eickrono-thimisu-app/docs/entendimento_foto_perfil_cadastro_login.md`

Problema:

- documento de entendimento ainda pode ter trechos de diagnostico sobre campos
  legados.

Acao:

- manter apenas como apoio de leitura do app;
- para implementacao, seguir este plano e o DBML alvo.

## Criterios de aceite

O trabalho so pode ser considerado pronto quando:

- autenticar social sem conta local nao cria usuario orfao;
- cadastro final cria pessoa, usuario, contatos, formas sociais e avatars;
- entrar-e-vincular salva a rede social apenas apos login local valido;
- Google e Apple podem ficar vinculados ao mesmo usuario;
- avatar preferido nao apaga redes sociais;
- foto local aparece para outros usuarios por URL publica/controlada;
- login por senha retorna avatar preferido;
- login social retorna avatar preferido;
- app atualiza cache local por versao/hash/data;
- app usa sigla quando nao existe avatar;
- HML foi validado em iPhone fisico;
- documentos conflitantes foram atualizados ou marcados como historicos.

## Pendencias de decisao antes de codificar

Todas as perguntas abaixo devem ser respondidas antes de codificar. Cada uma
tem opcoes para evitar ambiguidade.

### 1. Como a foto local do dispositivo deve chegar ao servidor?

Opcao A - app envia a imagem no POST para o `eickrono-autenticacao-servidor`.

- O app Thimisu envia a imagem junto com o payload final do cadastro.
- O `eickrono-autenticacao-servidor` valida o fluxo e repassa a imagem ao
  `eickrono-identidade-servidor`.
- O `eickrono-identidade-servidor` grava o arquivo/storage, gera `url_avatar`
  e cria o registro em `identidade.avatar_usuario`.
- Vantagem: o app conversa com um unico servidor no fechamento do cadastro.
- Cuidado: o `eickrono-autenticacao-servidor` vira passagem de arquivo e precisa
  ter limite de tamanho, timeout e validacao de content type.

Opcao B - app envia a imagem direto para o `eickrono-identidade-servidor`.

- O app Thimisu envia a imagem ao servidor que e dono do avatar.
- O `eickrono-autenticacao-servidor` continua cuidando do fluxo de autenticacao.
- Vantagem: arquivo entra direto no dominio que vai persistir avatar.
- Cuidado: o app passa a conversar com dois servidores durante o cadastro.

Opcao C - app usa URL preassinada de storage.

- Um servidor gera uma URL temporaria de upload.
- O app envia a imagem direto ao storage.
- Depois o app envia ao servidor a referencia do arquivo.
- Vantagem: servidores nao trafegam arquivo pesado.
- Cuidado: e mais complexo para a primeira implementacao.

Opcao recomendada para a primeira entrega: Opcao A, porque bate com o fluxo
esperado pelo app hoje e mantem o fechamento do cadastro concentrado no
`eickrono-autenticacao-servidor`.

Decisao aprovada: Opcao A.

### 2. Como o `eickrono-autenticacao-servidor` deve chamar o `eickrono-identidade-servidor`?

Opcao A - JWT interno servidor-servidor.

- O `eickrono-autenticacao-servidor` assina uma credencial interna para chamar
  o `eickrono-identidade-servidor`.
- Esse JWT nunca e enviado ao app.
- Serve apenas para comunicacao entre servidores.
- Vantagem: deixa claro que app nao participa dessa confianca interna.

Opcao B - OAuth2 client credentials entre servidores.

- O `eickrono-autenticacao-servidor` usa credencial de cliente para obter token
  de servico.
- Vantagem: padrao conhecido para comunicacao machine-to-machine.
- Cuidado: exige infraestrutura de emissao/validacao de token de servico.

Opcao C - assinatura HMAC por requisicao interna.

- Cada chamada interna recebe uma assinatura calculada com segredo compartilhado.
- Vantagem: simples para poucos servidores.
- Cuidado: rotacao de segredo e auditoria ficam mais manuais.

Decisao aprovada: Opcao A. O JWT existe na comunicacao interna entre
`eickrono-autenticacao-servidor` e `eickrono-identidade-servidor`, nao entre app
e `eickrono-autenticacao-servidor`.

### 3. Como normalizar telefone?

Opcao A - E.164 obrigatorio.

- Exemplo: `+5511999999999`.
- Melhor para unicidade e integracao.
- Exige parse correto por pais.

Opcao B - guardar DDI, DDD e numero separados.

- Facilita UI brasileira.
- Mais trabalhoso para unicidade global.

Opcao C - guardar os dois: campos separados e `telefone_normalizado`.

- Melhor para busca e exibicao.
- Mais campos no banco.

Opcao recomendada inicial: Opcao C, com `telefone_normalizado` unico quando
verificado.

Decisao aprovada: Opcao C.

### 4. Como representar as origens de avatar?

Opcao A - somente provedores/produtos atuais.

- Valores iniciais: `GOOGLE`, `APPLE`, `THIMISU`.
- Simples e suficiente para a primeira entrega.

Opcao B - provedores/produtos atuais mais `OUTRO`.

- Facilita integracoes futuras.
- Pode esconder origem mal modelada.

Opcao C - tabela de catalogo de origens.

- Mais flexivel.
- Mais trabalho agora.

Decisao aprovada: Opcao C.

### 5. Qual estrategia de rollback usar para campos antigos de avatar?

Opcao A - manter campos antigos por uma release.

- Backfill grava novo modelo.
- Codigo novo le novo modelo.
- Campos antigos continuam existindo ate HML/producao validar.

Opcao B - remover campos antigos na mesma entrega.

- Banco fica limpo mais rapido.
- Maior risco de quebrar consumidores antigos.

Opcao C - manter campos antigos como espelho temporario.

- Novo modelo e legado recebem escrita por um periodo.
- Reduz risco de rollback.
- Aumenta complexidade e risco de divergencia.

Opcao recomendada inicial: Opcao A, com remocao em etapa separada depois de
validacao em HML e iPhone fisico.

Decisao aprovada: Opcao A.

### 6. Qual campo o app deve usar para decidir se atualiza o cache do avatar?

Opcao A - usar somente `versao`.

- O app compara a versao recebida na sessao com a versao salva localmente.
- Se mudou, baixa/salva o novo avatar.
- Se e igual, mantem o cache local.
- Cuidado: exige que todo avatar sempre tenha versao confiavel.

Opcao B - usar somente `hash_conteudo`.

- O app compara o hash do arquivo.
- Melhor para detectar mudanca real de imagem.
- Cuidado: nem toda origem externa fornece hash nativo; o servidor teria que
  calcular ou derivar.

Opcao C - usar somente `atualizado_em`.

- O app compara a data da ultima atualizacao.
- Simples de implementar.
- Cuidado: data pode mudar sem mudanca real da imagem.

Opcao D - usar `versao` como principal e `url_avatar`/`atualizado_em` como
fallback.

- O app usa `avatarPreferidoVersao` como regra principal.
- Se a versao vier igual, mantem o cache local.
- Se a versao vier diferente, atualiza o avatar local.
- Se a versao nao existir por compatibilidade antiga, compara `url_avatar`.
- Se ainda houver duvida, usa `avatarPreferidoAtualizadoEm` como fallback.

Decisao aprovada: Opcao D.
