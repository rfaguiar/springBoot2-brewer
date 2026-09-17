# 03 · Estrutura de Pastas

[⬅ Voltar ao README](../Readme.md)

## Visão geral do repositório

```
springBoot2-brewer/
├── Dockerfile                  # Build multi-stage da imagem da aplicação
├── docker-compose.yml          # Stack local: app + MySQL
├── docker-entrypoint.sh        # Aguarda o MySQL ficar disponível antes de subir a app
├── pom.xml                     # Build Maven / dependências
├── .travis.yml                 # Pipeline de CI (build + SonarCloud)
├── docs/                       # Esta documentação
├── .azure/                     # Plano de containerização gerado por ferramenta de modernização
├── .github/modernize/          # Configuração/relatórios de assessment (AppCAT, upgrade Java)
└── src/
    ├── main/java/com/brewer/   # Código-fonte da aplicação
    └── main/resources/         # Configurações, templates, migrações, i18n
```

## Pacotes Java (`src/main/java/com/brewer`)

| Pacote | Responsabilidade | Exemplos de classes |
|---|---|---|
| `com.brewer` (raiz) | Bootstrap da aplicação e constantes globais | `BrewerApplication`, `Constantes` |
| `controller` | Endpoints MVC (recebem requisições HTTP e retornam `ModelAndView`) | `CervejasController`, `VendasController`, `DashboardController`, `SegurancaController`, `FotosController` |
| `controller.converter` | Conversão de valores de formulário (ex.: id → entidade) | `CidadeConverter`, `EstadoConverter`, `EstiloConverter`, `GrupoConverter` |
| `controller.handler` | Tratamento centralizado de exceções | `ControllerAdviceExceptionHandler` |
| `controller.page` | Suporte à paginação exibida nas views | `PageWrapper` |
| `controller.validator` | Validações de negócio específicas de um formulário | `VendaValidator` |
| `service` | Regras de negócio / casos de uso (camada de aplicação) | `CadastroCervejaService`, `CadastroVendaService`, `CadastroUsuarioService`, `CadastroClienteService` |
| `service.event.venda` | Evento de domínio disparado ao emitir uma venda | `VendaEvent`, `VendaListener` |
| `service.exception` | Exceções de negócio (traduzidas em mensagens amigáveis pelo handler) | `VendaException`, `EmailUsuarioJaCadastradoException`, `CpfCnpjClienteJaCadastradoException` |
| `repository` | Interfaces Spring Data JPA (contratos de acesso a dados) | `Cervejas`, `Clientes`, `Vendas`, `Usuarios`, `Grupos` |
| `repository.filter` | Objetos de filtro usados nas telas de pesquisa | `VendaFilter`, `ClienteFilter`, `CervejaFilter` |
| `repository.helper.*` | Implementações customizadas de consultas dinâmicas (Criteria API/JPQL) por agregado | `helper.venda.VendasImpl`, `helper.cerveja.CervejasImpl`, ... |
| `repository.paginacao` | Utilitário de paginação para as consultas customizadas | `PaginacaoUtil` |
| `repository.listener` | Listener de ciclo de vida de entidade JPA | `CervejaEntityListener` |
| `model` | Entidades JPA e enums de domínio | `Cerveja`, `Cliente`, `Venda`, `Usuario`, `Grupo`, `Permissao`, `StatusVenda`, `TipoPessoa`, `Origem` |
| `model.validation` | Validação condicional de grupos Bean Validation (CPF vs. CNPJ) | `ClienteGroupSequenceProvider` |
| `dto` | Objetos de transporte de dados para views/relatórios | `CervejaDTO`, `VendaMes`, `VendaOrigem`, `FotoDTO`, `ValorItensEstoque` |
| `config` | Beans de infraestrutura e formatação | `SecurityConfig`, `MailConfig`, `S3Config`, `WebConfig`, `GeracaoDeSenha`, `config.format.*` |
| `security` | Integração com Spring Security | `AppUserDetailsService`, `UsuarioSistema` |
| `session` | Estado de carrinho de compras por sessão HTTP | `TabelaItensVenda`, `TabelasItensSession` |
| `storage` | Abstração e implementações de armazenamento de fotos | `FotoStorage`, `storage.local.FotoStorageLocal`, `storage.s3.FotoStorageS3` |
| `validation` | Anotações e validadores customizados | `SKU`, `AtributoConfirmacao`, `validator.AtributoConfirmacaoValidator` |
| `mail` | Envio de e-mail de confirmação de venda | `Mailer` |
| `thymeleaf` | Extensão da linguagem de templates | `BrewerDialect`, `thymeleaf.processor.*` (menu, paginação, mensagens, ordenação) |

## Recursos (`src/main/resources`)

| Caminho | Conteúdo |
|---|---|
| `application.properties` | Configuração padrão (perfil local/dev) |
| `application-prod.properties` | Sobrescrita de configuração para produção (SendGrid, S3, banco via env) |
| `db/migration` | Scripts de migração de banco (Flyway) |
| `messages.properties` / `messages_pt_BR.properties` | Internacionalização de mensagens |
| `logback-spring.xml` | Configuração de logging |
| `static` | Assets estáticos (CSS/JS/imagens) |
| `templates` | Views Thymeleaf (HTML) |
| `env` | Variáveis auxiliares de ambiente |

## Próxima leitura

- [02 · Arquitetura](02-arquitetura.md)
- [04 · Catálogo Tecnológico](04-catalogo-tecnologico.md)
