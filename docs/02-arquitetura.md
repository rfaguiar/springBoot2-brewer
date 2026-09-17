# 02 · Arquitetura

[⬅ Voltar ao README](../Readme.md)

## Estilo arquitetural

O projeto segue uma **arquitetura em camadas (Layered Architecture)** clássica de aplicações
Spring MVC "server-side rendered" (SSR com Thymeleaf), sem separação físico de frontend/backend:

```
┌──────────────────────────────────────────────────────────────────┐
│  Camada Web (Thymeleaf + BrewerDialect)                          │
│  Templates HTML renderizados no servidor (src/main/resources/    │
│  templates), com dialect customizado para menus, paginação, etc. │
└───────────────▲────────────────────────────────────────────────--┘
                 │ ModelAndView
┌───────────────┴──────────────────────────────────────────────────┐
│  Controller (com.brewer.controller)                               │
│  Recebe requisições HTTP, valida entrada (Validator/BindingResult),│
│  delega regra de negócio ao Service, monta o Model/View            │
└───────────────▲──────────────────────────────────────────────────-┘
                 │
┌───────────────┴──────────────────────────────────────────────────┐
│  Service (com.brewer.service)                                      │
│  Regras de negócio, orquestração de casos de uso, publica eventos  │
│  de domínio (ApplicationEventPublisher), lança exceções de negócio │
└───────────────▲──────────────────────────────────────────────────-┘
                 │
┌───────────────┴──────────────────────────────────────────────────┐
│  Repository (com.brewer.repository + .helper)                      │
│  Spring Data JPA (CRUD) + implementações customizadas ("Impl") com │
│  Criteria API/JPQL para filtros dinâmicos e relatórios              │
└───────────────▲──────────────────────────────────────────────────-┘
                 │
┌───────────────┴──────────────────────────────────────────────────┐
│  Model / Domínio (com.brewer.model)                                │
│  Entidades JPA (Cerveja, Cliente, Venda, Usuario, ...) com regras  │
│  de validação (Bean Validation) e comportamento (ex.: Venda        │
│  calcula seu próprio total)                                       │
└────────────────────────────────────────────────────────────────--┘
```

Camadas transversais (cross-cutting) usadas em todo o fluxo: **Security** (autenticação/autorização),
**Config** (beans de infraestrutura), **Session** (estado de carrinho de compras), **Storage**
(persistência de arquivos), **Mail** (envio de e-mail) e **Thymeleaf** (extensões de view).

## Padrões de projeto aplicados

| Padrão | Onde é usado | Objetivo |
|---|---|---|
| **MVC (Model-View-Controller)** | Todo o pacote `controller` + templates Thymeleaf | Separar apresentação, controle de fluxo HTTP e domínio |
| **Repository** | `com.brewer.repository.*` | Abstrair acesso a dados por trás de interfaces (`Cervejas`, `Vendas`, `Usuarios`, ...) |
| **Strategy** | `com.brewer.storage.FotoStorage` com implementações `FotoStorageLocal` e `FotoStorageS3` | Trocar a estratégia de armazenamento de fotos (disco local vs. Amazon S3) sem alterar quem consome |
| **DTO (Data Transfer Object)** | `com.brewer.dto.*` (`CervejaDTO`, `VendaMes`, `VendaOrigem`, `FotoDTO`) | Expor apenas os dados necessários para relatórios/APIs, sem vazar a entidade JPA |
| **Observer / Domain Event** | `VendaEvent` + `VendaListener` (`@EventListener`) | Desacoplar a baixa de estoque da emissão da venda — publicado via `ApplicationEventPublisher` |
| **Converter** | `com.brewer.controller.converter.*` (`CidadeConverter`, `EstadoConverter`, ...) | Converter `String`/id vindos de formulários HTML em entidades JPA automaticamente |
| **Validator customizado** | `com.brewer.controller.validator.VendaValidator`, `com.brewer.validation.*` | Validações de negócio que vão além de Bean Validation (ex.: venda sem itens) |
| **Chain of Responsibility / ControllerAdvice** | `ControllerAdviceExceptionHandler` | Tratamento centralizado de exceções de negócio, convertendo-as em mensagens amigáveis na view |
| **Session-scoped Component** | `TabelasItensSession` (`@SessionScope`) | Manter o "carrinho" de itens de uma venda em andamento por sessão HTTP, antes de persistir |
| **Dialect / Tag Processor (Thymeleaf)** | `BrewerDialect` + `com.brewer.thymeleaf.processor.*` | Estender a linguagem de templates com tags próprias (menu ativo, paginação, mensagens i18n, ordenação de colunas) |
| **Specification/Filter dinâmico** | `com.brewer.repository.filter.*` + `helper.*Impl` (Criteria API) | Montar queries de pesquisa com filtros opcionais combináveis |

## Fluxo típico de uma requisição (exemplo: emitir uma venda)

1. Usuário submete o formulário de venda → `VendasController#emitir`.
2. Controller valida a venda (`VendaValidator`) e delega a `CadastroVendaService#emitir`.
3. Service persiste a venda via `Vendas` (repository) e publica um `VendaEvent`.
4. `VendaListener` escuta o evento e dá baixa no estoque de cada `Cerveja` vendida.
5. Controller redireciona com mensagem de sucesso (`RedirectAttributes` + flash message).
6. Opcionalmente, `Mailer` envia e-mail de confirmação com PDF gerado via JasperReports em anexo.

## Segurança e sessão

Consulte o detalhamento em [06 · Segurança](06-seguranca.md).

## Próxima leitura

- [03 · Estrutura de Pastas](03-estrutura-de-pastas.md)
- [07 · Persistência e Dados](07-persistencia-e-dados.md)
