# 05 · Features

[⬅ Voltar ao README](../Readme.md)

Funcionalidades de negócio implementadas, organizadas por módulo/tela.

## Cervejas (produtos)

- CRUD completo (`CervejasController` + `CadastroCervejaService`);
- Upload de foto do produto com geração automática de **thumbnail** (Thumbnailator);
- Armazenamento de foto **local** (disco) ou em **Amazon S3**, conforme configuração ativa
  (padrão *Strategy* — ver [02 · Arquitetura](02-arquitetura.md));
- Validação de **SKU** único por meio de anotação customizada (`@SKU`);
- Controle de **estoque** (`quantidadeEstoque`), atualizado automaticamente ao emitir uma venda;
- Pesquisa filtrada por nome, estilo e faixa de valor (`CervejaFilter`), com paginação.

## Clientes

- CRUD de clientes pessoa física (CPF) ou jurídica (CNPJ);
- Validação condicional de campos obrigatórios conforme o tipo de pessoa
  (`ClienteGroupSequenceProvider`, grupos `CpfGroup`/`CnpjGroup`);
- Impede cadastro de CPF/CNPJ duplicado (`CpfCnpjClienteJaCadastradoException`);
- Endereço vinculado com busca de cidade/estado.

## Cidades e Estados

- Cadastro de cidades vinculadas a um estado;
- Impede nome de cidade duplicado no mesmo estado (`NomeCidadeJaCadastradaException`);
- Conversores dedicados (`CidadeConverter`, `EstadoConverter`) para uso em formulários HTML.

## Estilos de cerveja

- Cadastro de estilos (ex.: IPA, Lager, Stout);
- Impede nome de estilo duplicado (`NomeEstiloJaCadastradoException`).

## Usuários, Grupos e Permissões (RBAC)

- Cadastro de usuários com associação a **grupos** (`UsuarioGrupo`);
- Cada grupo agrega um conjunto de **permissões** (`Permissao`), usadas pelo Spring Security para
  autorização por papel/role (ver [06 · Segurança](06-seguranca.md));
- Geração/definição de senha do usuário com criptografia BCrypt (`GeracaoDeSenha`);
- Impede e-mail de usuário duplicado (`EmailUsuarioJaCadastradoException`) e valida senha obrigatória
  em determinados fluxos (`SenhaObrigatoriaUsuarioException`).

## Vendas

Fluxo central do sistema, orquestrado por `VendasController` + `CadastroVendaService`:

- **Carrinho de itens em sessão** (`TabelasItensSession`/`TabelaItensVenda`, `@SessionScope`):
  adicionar, alterar quantidade e excluir itens antes de salvar a venda, identificado por um `uuid`
  de rascunho;
- **Salvar** venda como rascunho ou **emitir** definitivamente (baixa de estoque via evento de
  domínio `VendaEvent`/`VendaListener`);
- **Cancelar** venda emitida (com verificação de permissão, retornando página 403 se negado);
- **Enviar e-mail** de confirmação com **PDF em anexo** gerado via JasperReports (`Mailer`);
- Pesquisa de vendas com filtros (status, período, cliente, tipo de pessoa) e paginação
  (`VendaFilter` + `PageWrapper`);
- Cálculo automático de valor total do pedido (subtotal + frete − desconto) feito pela própria
  entidade `Venda`.

## Dashboard

- Endpoints JSON (`@ResponseBody`) consumidos por gráficos na tela inicial:
  - `GET /vendas/totalPorMes` — total de vendas agrupado por mês (`VendaMes`);
  - `GET /vendas/porOrigem` — total de vendas agrupado por origem nacional/estrangeira (`VendaOrigem`).

## Internacionalização (i18n)

- Mensagens de UI em `pt_BR` (`messages_pt_BR.properties`) com fallback padrão
  (`messages.properties`);
- Locale fixo em português (`spring.mvc.locale=pt_BR`);
- Formatação customizada de `LocalDate`, `LocalDateTime`, `LocalTime`, `BigDecimal` e `Integer`
  conforme padrão brasileiro (`com.brewer.config.format.*`).

## Extensões de template (Thymeleaf)

Dialect customizado `brewer:` (`BrewerDialect`) usado nas views para:

- Marcar item de **menu ativo** conforme a URL corrente (`MenuAttributeTagProcessor`);
- Exibir **mensagens** de validação/flash de forma padronizada (`MessageElementTagProcessor`);
- Gerar **links de ordenação** de colunas em tabelas de pesquisa (`OrderElementTagProcessor`);
- Renderizar **paginação** (`PaginationElementTagProcessor`);
- Aplicar classe CSS de erro em campos inválidos (`ClassForErrorAttributeTagProcessor`).

## Próxima leitura

- [06 · Segurança](06-seguranca.md)
- [07 · Persistência e Dados](07-persistencia-e-dados.md)
