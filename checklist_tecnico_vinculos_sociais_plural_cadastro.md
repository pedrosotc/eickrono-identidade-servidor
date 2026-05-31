# Checklist Técnico: Vínculos Sociais e Avatares no Cadastro

## Premissa canônica

O cadastro não deve persistir contexto social pendente no backend. A autenticação
social feita antes do cadastro fica como dado temporário no app até o cadastro
ser enviado. O backend só persiste vínculo social depois que existe pessoa/usuário
definitivo.

Regras duras:

- o cadastro envia listas confirmadas de vínculos sociais e avatares;
- cada vínculo social confirmado deve ser materializado sem competir por um campo
  singular;
- apenas um avatar pode ficar com `preferido = true` por usuário/projeto;
- foto carregada do dispositivo deve ser enviada ao backend junto com as demais
  opções de avatar, mesmo quando não for a preferida;
- o contrato final usa somente listas confirmadas para vínculos sociais de
  cadastro.

## Contrato esperado

`CadastroApiRequest` deve aceitar apenas:

- `vinculosSociaisConfirmados`: lista de redes sociais já autenticadas pelo app e
  incluídas no cadastro final;
- `avataresCadastroConfirmados`: lista de opções de avatar do cadastro, incluindo
  URLs sociais e upload local/controlado;
- dados comuns do cadastro local: usuário, e-mail, telefone, senha, aceite de
  termos e atestação.

Campos removidos do contrato:

- payload singular de vínculo social;
- payload de pendência social no servidor;
- identificador de contexto social persistido.

## Persistência esperada

Ao finalizar o cadastro:

- o serviço cria/localiza a pessoa e o usuário central;
- cada vínculo social confirmado é persistido em forma de acesso/vínculo social;
- cada avatar confirmado é persistido na tabela canônica de avatar;
- o avatar marcado como preferido atualiza a preferência visual do usuário no
  projeto;
- se nenhuma opção vier como preferida, o app continua usando sigla/avatar padrão.

## Testes obrigatórios

- cadastro comum sem rede social não envia nem grava vínculo social;
- cadastro com Google e Apple grava os dois vínculos;
- cadastro com avatar social preferido grava somente aquele avatar como
  `preferido = true`;
- cadastro com foto local enviada grava a opção de avatar local/controlada;
- contrato público não aceita nem produz identificador de contexto social
  persistido em servidor;
- login local não consome contexto social pendente;
- nenhum teste deve depender de tabela de contexto social pendente.

## Arquivos de referência

- `CadastroApiRequest.java`
- `FluxoPublicoController.java`
- `CadastroContaInternaServico.java`
- `CadastroConta.java`
- `VinculoSocialService.java`
- migrations de avatar e remoção de contexto social pendente
