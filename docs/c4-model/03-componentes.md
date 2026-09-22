# 03 · Diagrama de Componentes (C4 Nível 3)

[⬅ Voltar ao Modelo C4](README.md)

Faz o zoom dentro do contêiner **Aplicação Web**, agrupando as classes de
`src/main/java/com/brewer` por responsabilidade (pacote). Cada caixa é um componente lógico
(no sentido de "conjunto coeso de classes"), não uma classe isolada.

```mermaid
flowchart TB
    colaborador["👤 Colaborador<br/><i>[Pessoa]</i>"]

    subgraph webapp["Aplicação Web [Contêiner: Spring Boot]"]
        direction TB

        subgraph camadaWeb["Camada Web (com.brewer.controller.*)"]
            controllers["Controllers<br/><i>[@Controller]</i><br/>CervejasController, ClientesController,<br/>VendasController, UsuariosController,<br/>DashboardController, SegurancaController,<br/>FotosController, CidadesController, EstilosController"]
            converters["Converters<br/><i>[controller.converter]</i><br/>CidadeConverter, EstadoConverter,<br/>EstiloConverter, GrupoConverter"]
            validators["Validators<br/><i>[controller.validator]</i><br/>VendaValidator"]
            handler["Exception Handler<br/><i>[controller.handler]</i><br/>ControllerAdviceExceptionHandler"]
        end

        dialect["BrewerDialect<br/><i>[com.brewer.thymeleaf]</i><br/>Tags customizadas: menu ativo, paginação,<br/>mensagens i18n, ordenação de colunas"]

        subgraph camadaSeguranca["Segurança (com.brewer.security / config)"]
            security["AppUserDetailsService / UsuarioSistema<br/><i>[Spring Security]</i><br/>Autenticação por e-mail + RBAC dinâmico<br/>(Grupo → Permissao)"]
            secconfig["SecurityConfig<br/><i>[@Configuration]</i><br/>SecurityFilterChain, regras por rota,<br/>sessão única (maximumSessions=1)"]
        end

        subgraph camadaAplicacao["Camada de Aplicação (com.brewer.service.*)"]
            services["Services<br/><i>[@Service]</i><br/>CadastroCervejaService, CadastroVendaService,<br/>CadastroClienteService, CadastroUsuarioService,<br/>CadastroCidadeService, CadastroEstiloService"]
            evento["VendaEvent / VendaListener<br/><i>[service.event.venda]</i><br/>Baixa de estoque desacoplada<br/>da emissão da venda (Observer)"]
            sessioncomp["TabelasItensSession<br/><i>[com.brewer.session, @SessionScope]</i><br/>Carrinho de itens de uma venda em andamento"]
        end

        subgraph camadaPersistencia["Persistência (com.brewer.repository.*)"]
            repos["Repositories<br/><i>[Spring Data JPA]</i><br/>Cervejas, Clientes, Vendas, Usuarios,<br/>Grupos, Cidades, Estados, Estilos"]
            helpers["Repository Helpers<br/><i>[repository.helper.*Impl]</i><br/>Criteria API/JPQL: filtros dinâmicos,<br/>relatórios e paginação (PaginacaoUtil)"]
        end

        model["Entidades JPA<br/><i>[com.brewer.model]</i><br/>Cerveja, Cliente, Venda, Usuario, Grupo,<br/>Permissao, Cidade, Estado, Estilo, ItemVenda..."]

        subgraph camadaInfra["Infraestrutura Transversal"]
            storage["FotoStorage<br/><i>[com.brewer.storage, Strategy]</i><br/>FotoStorageLocal / FotoStorageS3"]
            mailer["Mailer<br/><i>[com.brewer.mail]</i><br/>Monta e envia e-mail de confirmação"]
            jasper["Relatório de Venda<br/><i>[JasperReports]</i><br/>Gera o PDF anexado ao e-mail"]
        end
    end

    db[("MySQL 8<br/><i>[Contêiner]</i>")]
    localdisk["Armazenamento Local<br/><i>[Contêiner]</i>"]
    s3["Amazon S3<br/><i>[Sistema Externo]</i>"]
    sendgrid["SendGrid<br/><i>[Sistema Externo]</i>"]

    colaborador --> controllers
    controllers --> converters
    controllers --> validators
    controllers --> handler
    controllers -.->|renderiza via| dialect
    controllers --> secconfig
    controllers --> services
    security --> repos
    secconfig --> security

    services --> evento
    services --> sessioncomp
    services --> repos
    services --> storage
    services --> mailer

    repos --> helpers
    repos --> model
    repos --> db

    storage --> localdisk
    storage --> s3

    mailer --> jasper
    mailer --> sendgrid

    classDef person fill:#08427b,color:#fff,stroke:#052e56;
    classDef component fill:#85bbf0,color:#000,stroke:#4d7fa3;
    classDef container fill:#1168bd,color:#fff,stroke:#0b4884;
    classDef db fill:#438dd5,color:#fff,stroke:#2e6295;
    classDef external fill:#999999,color:#fff,stroke:#6b6b6b;

    class colaborador person
    class controllers,converters,validators,handler,dialect,security,secconfig,services,evento,sessioncomp,repos,helpers,model,storage,mailer,jasper component
    class db db
    class localdisk container
    class s3,sendgrid external
```

## Componentes por camada

| Grupo | Pacote(s) | Componentes principais | Padrão de projeto |
|---|---|---|---|
| Camada Web | `controller`, `controller.converter/handler/validator` | `*Controller`, `*Converter`, `VendaValidator`, `ControllerAdviceExceptionHandler` | MVC, Converter, Validator, Chain of Responsibility |
| Extensão de View | `thymeleaf`, `thymeleaf.processor` | `BrewerDialect` | Dialect/Tag Processor |
| Segurança | `security`, `config` (`SecurityConfig`) | `AppUserDetailsService`, `UsuarioSistema` | Adapter (domínio ↔ Spring Security) |
| Aplicação | `service`, `service.event.venda`, `session` | `Cadastro*Service`, `VendaEvent`/`VendaListener`, `TabelasItensSession` | Observer/Domain Event, Session-scoped Component |
| Persistência | `repository`, `repository.filter`, `repository.helper.*`, `repository.paginacao` | `Cervejas`, `Vendas`, `*Impl`, `PaginacaoUtil` | Repository, Specification/Filtro dinâmico |
| Domínio | `model`, `model.validation` | `Cerveja`, `Venda`, `Usuario`, `ClienteGroupSequenceProvider` | Rich Domain Model |
| Infraestrutura transversal | `storage`, `mail`, `config` | `FotoStorage` (+ `local`/`s3`), `Mailer`, JasperReports | Strategy |

Ver detalhamento completo de cada padrão em [02 · Arquitetura](../02-arquitetura.md#padrões-de-projeto-aplicados).

## Próxima leitura

- [04 · Diagrama Dinâmico (emissão de venda)](04-dinamico.md)
- [02 · Diagrama de Contêineres](02-containers.md)
