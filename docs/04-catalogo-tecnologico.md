# 04 · Catálogo Tecnológico

[⬅ Voltar ao README](../Readme.md)

> Tabela de referência rápida de **todas as tecnologias/bibliotecas** usadas no projeto, extraída
> diretamente do `pom.xml`. Use esta página como "cola de estudos" para lembrar rapidamente para que
> serve cada dependência de um projeto Spring Boot típico.

## Plataforma

| Tecnologia | Versão | Categoria | Para que serve |
|---|---|---|---|
| [Java](https://openjdk.org/) | 25 | Linguagem/Runtime | Linguagem e JDK usados para compilar e rodar a aplicação |
| [Spring Boot](https://spring.io/projects/spring-boot) (`spring-boot-starter-parent`) | 4.1.1 | Framework | BOM/parent que gerencia versões de todas as dependências Spring e fornece autoconfiguração |
| [Apache Maven](https://maven.apache.org/) | (wrapper/local) | Build tool | Gerenciamento de dependências, build e empacotamento (`mvn clean package`) |

## Web / MVC

| Tecnologia | Versão | Categoria | Para que serve |
|---|---|---|---|
| `spring-boot-starter-webmvc` | (gerenciado pelo Boot) | Web | Monta o servidor web embarcado (Tomcat) e a infraestrutura Spring MVC (`@Controller`, `ModelAndView`) |
| `spring-boot-starter-validation` | (gerenciado pelo Boot) | Validação | Bean Validation (Jakarta Validation) para `@Valid`/`@NotNull`/`@Size`, etc. |
| [Thymeleaf](https://www.thymeleaf.org/) (`spring-boot-starter-thymeleaf`) | (gerenciado pelo Boot) | Template engine | Renderização de HTML no servidor (SSR) a partir de `src/main/resources/templates` |
| [thymeleaf-layout-dialect](https://github.com/ultraq/thymeleaf-layout-dialect) | 3.3.0 | Template engine | Permite templates com layout/herança (`layout:decorate`) |
| [thymeleaf-extras-data-attribute](https://github.com/mxab/thymeleaf-extras-data-attribute) | 2.0.1 | Template engine | Facilita a geração de atributos `data-*` em elementos HTML a partir de mapas Java |
| [thymeleaf-extras-springsecurity6](https://github.com/thymeleaf/thymeleaf-extras-springsecurity6) | 3.1.5.RELEASE | Template engine + Segurança | Tags Thymeleaf que consultam o contexto de segurança (ex.: `sec:authorize`) |

## Persistência de dados

| Tecnologia | Versão | Categoria | Para que serve |
|---|---|---|---|
| `spring-boot-starter-data-jpa` | (gerenciado pelo Boot) | Persistência | Integração com JPA/Hibernate, repositórios Spring Data |
| [MySQL Connector/J](https://dev.mysql.com/doc/connector-j/en/) (`com.mysql:mysql-connector-j`) | (gerenciado pelo Boot) | Driver JDBC | Driver de conexão com o banco MySQL |
| [Flyway](https://flywaydb.org/) (`spring-boot-starter-flyway` + `flyway-mysql`) | (gerenciado pelo Boot) | Migração de banco | Versiona e aplica scripts SQL de schema/dados automaticamente na inicialização |
| [EhCache](https://www.ehcache.org/) + JCache (`javax.cache:cache-api`) | 1.1.1 (cache-api) | Cache | Cache de segundo nível/consultas para reduzir acesso ao banco |

## Segurança

| Tecnologia | Versão | Categoria | Para que serve |
|---|---|---|---|
| [Spring Security](https://spring.io/projects/spring-security) (`spring-boot-starter-security`) | (gerenciado pelo Boot) | Segurança | Autenticação (form login), autorização por papéis/permissões, proteção de sessão |
| `BCryptPasswordEncoder` (parte do Spring Security) | — | Segurança | Hash seguro de senhas de usuário |

## Armazenamento e arquivos

| Tecnologia | Versão | Categoria | Para que serve |
|---|---|---|---|
| [AWS SDK for Java (S3)](https://aws.amazon.com/sdk-for-java/) (`aws-java-sdk-s3`) | 1.12.261 | Cloud Storage | Upload/leitura de fotos de produtos no Amazon S3 (estratégia alternativa ao disco local) |
| [Thumbnailator](https://github.com/coobird/thumbnailator) | 0.4.8 | Processamento de imagem | Geração de thumbnails das fotos de cerveja |

## E-mail e relatórios

| Tecnologia | Versão | Categoria | Para que serve |
|---|---|---|---|
| `spring-boot-starter-mail` | (gerenciado pelo Boot) | E-mail | Envio de e-mails (SMTP/SendGrid) com confirmação de venda |
| [JasperReports](https://community.jaspersoft.com/) (`jasperreports` + `jasperreports-fonts`) | 7.0.7 | Relatórios | Geração de PDF de venda anexado ao e-mail de confirmação |
| [commons-beanutils](https://commons.apache.org/proper/commons-beanutils/) | 1.11.0 | Utilitário | Dependência de suporte do JasperReports (pinada explicitamente por CVE-2025-48734) |

## Utilitários gerais

| Tecnologia | Versão | Categoria | Para que serve |
|---|---|---|---|
| [Apache Commons Lang3](https://commons.apache.org/proper/commons-lang/) | (gerenciado pelo Boot) | Utilitário | Funções utilitárias de String, número, reflexão, etc. |
| Jakarta XML Bind + `jaxb-runtime` (Glassfish) | (gerenciado pelo Boot) | XML/JAXB | Necessário para o Hibernate ler `META-INF/orm.xml` em runtimes Java modernos |
| `org.glassfish.expressly` | 6.0.0 (escopo teste) | EL (Expression Language) | Implementação Jakarta EL usada em testes que renderizam Thymeleaf |

## Testes e qualidade

| Tecnologia | Versão | Categoria | Para que serve |
|---|---|---|---|
| [JUnit 4](https://junit.org/junit4/) | 4.12 | Testes | Framework de testes unitários |
| [Mockito](https://site.mockito.org/) (`mockito-core`) | (gerenciado pelo Boot) | Testes | Mocks/stubs para isolar unidades sob teste |
| [AssertJ](https://assertj.github.io/doc/) | (gerenciado pelo Boot) | Testes | Assertions fluentes e legíveis |
| [Hamcrest](https://hamcrest.org/) (`hamcrest-library`) | 1.3 | Testes | Matchers complementares de asserção |
| [EqualsVerifier](https://jqno.nl/equalsverifier/) | 3.17.5 | Testes | Testa contratos de `equals`/`hashCode` das entidades de forma automatizada |
| [H2 Database](https://www.h2database.com/) | (gerenciado pelo Boot, escopo teste) | Banco em memória | Banco leve para testes de integração sem depender do MySQL |
| `spring-test` | (gerenciado pelo Boot) | Testes | Suporte a testes de contexto Spring/MVC |
| [PIT / pitest-maven](https://pitest.org/) | 1.19.1 | Mutation Testing | Mede a *qualidade* dos testes (não só cobertura de linha) — ver [09 · Qualidade e Testes](09-qualidade-e-testes.md) |
| `maven-surefire-plugin` | (gerenciado pelo Boot) | Build/Testes | Executa os testes durante o build Maven |

## Empacotamento e execução

| Tecnologia | Versão | Categoria | Para que serve |
|---|---|---|---|
| `spring-boot-maven-plugin` | (gerenciado pelo Boot) | Build | Empacota a aplicação como JAR executável (`java -jar`) |
| `spring-boot-devtools` | (gerenciado pelo Boot, escopo runtime) | Produtividade | Restart automático e live reload durante desenvolvimento |
| [Docker](https://www.docker.com/) / [Docker Compose](https://docs.docker.com/compose/) | — | Containerização | Empacotamento e execução da aplicação e do MySQL em containers — ver [08 · Execução Local e Docker](08-execucao-local-e-docker.md) |

## Próxima leitura

- [05 · Features](05-features.md)
- [08 · Execução Local e Docker](08-execucao-local-e-docker.md)
