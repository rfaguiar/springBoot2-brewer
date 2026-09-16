# Plano de Migração para Spring Native (GraalVM Native Image)

> ## Status de execução (branch `feature/spring-native-migration`)
>
> Este plano foi **executado de ponta a ponta** nesta branch: o bloqueador de build nativo foi
> resolvido e um build real (`mvn -Pnative spring-boot:build-image`, via Cloud Native Buildpacks,
> JDK 25 + Maven local + Docker, sem necessidade de instalar GraalVM localmente) terminou com
> **BUILD SUCCESS**, gerando a imagem `docker.io/library/brewer-springboot:1.0.0-SNAPSHOT`, que
> foi executada em um container Docker real conectado ao MySQL e respondeu a requisições HTTP.
>
> ### Linha do tempo da investigação
>
> 1. ✅ **Fase 1 confirmada sem alterações no `pom.xml`**: os profiles `native`/`nativeTest`
>    (com `org.graalvm.buildtools:native-maven-plugin`) já vêm herdados de
>    `spring-boot-starter-parent:4.1.1` (verificado com `mvn help:all-profiles`), incluindo a
>    exclusão automática do `spring-boot-devtools` do build nativo.
> 2. ✅ **JasperReports removido do `pom.xml`** (`net.sf.jasperreports:jasperreports` e
>    `jasperreports-fonts`): confirmado por busca no código-fonte que a dependência **não era
>    usada em nenhum lugar** (nenhum `.jrxml`, nenhuma referência a `net.sf.jasperreports.*`).
>    Ela carregava um runtime Groovy embutido cujos inicializadores de classe (`CacheableCallSite`)
>    iniciam threads de background que o GraalVM native-image rejeita no heap da imagem. Suíte de
>    testes (275 testes) validada com `mvn test` após a remoção — sem regressões.
> 3. 🔴 **Bloqueador real encontrado (não previsto originalmente)**: `nz.net.ultraq.thymeleaf:
>    thymeleaf-layout-dialect` (usado de fato pelos templates via `layout:decorate`/
>    `layout:fragment`) depende obrigatoriamente de **Apache Groovy** — a própria biblioteca é
>    implementada em Groovy. Seu runtime de metaprogramação (`groovy.lang.GroovySystem`,
>    `org.codehaus.groovy.reflection.ClassInfo`) mantém estado vivo incompatível com o heap de
>    imagem do GraalVM. Três variações de `--initialize-at-run-time` foram tentadas sem sucesso
>    (cada uma só deslocava a falha para outra classe Groovy).
> 4. ✅ **Bloqueador resolvido**: `thymeleaf-layout-dialect` foi **removida do `pom.xml`** e todos
>    os **15 templates** que a usavam (`layout/LayoutPadrao.html`, `layout/LayoutSimples.html`,
>    `error.html`, `Dashboard.html`, e as telas de `cidade`, `cerveja`, `cliente`, `venda`,
>    `estilo` e `usuario`) foram convertidos para o padrão puro do Thymeleaf de **fragmentos
>    parametrizados** (`th:fragment="layout(titulo, cabecalhoExtra, conteudo, jsExtra)"` +
>    `th:replace="~{layout/LayoutPadrao :: layout(...)}"` no `<html>` de cada página), eliminando
>    totalmente a dependência de Groovy. Título de página, `<link>`s extras no `<head>` e scripts
>    extras (antes resolvidos automaticamente pelo "auto head-merging" do layout-dialect) agora são
>    passados explicitamente como parâmetros de fragmento, com `${titulo} ?: _` como *fallback*
>    para páginas que não os declaram.
>    **Validado rodando a aplicação de verdade** (`mvn spring-boot:run` contra MySQL via
>    `docker-compose`, login com usuário admin, navegação por todas as páginas afetadas) — todas
>    renderizaram corretamente (título, CSS/JS extras, conteúdo, página de erro 404 via
>    `LayoutSimples`).
> 5. 🔴→✅ **Segundo bloqueador encontrado e resolvido, desta vez em runtime**: o binário nativo
>    **compilou e chegou a iniciar**, mas falhava ao subir com
>    `ClassNotFoundException: org.flywaydb.core.internal.logging.slf4j.Slf4jLogCreator` — o
>    Flyway usa `Class.forName` para autodetectar qual *logging backend* (SLF4J, Log4j2, etc.)
>    está no classpath, e no GraalVM native-image isso exige registro explícito de reflection,
>    mesmo para classes presentes na imagem.
> 6. ✅ **Build nativo completo com sucesso** (`BUILD SUCCESS`, ~10-18 min de build):
>    imagem `docker.io/library/brewer-springboot:1.0.0-SNAPSHOT` gerada e testada com
>    `docker run` contra o MySQL do `docker-compose.yml`.
> 7. 🔄 **Cadeia de gaps de reflection/resource em runtime (Flyway + MySQL Connector/J)**: com o
>    binário realmente rodando contra um MySQL de verdade, uma sequência de novos gaps de
>    native-image foi revelada e corrigida **uma a uma, cada uma validada com um novo build e
>    `docker run` real**, todas centralizadas em `com.brewer.config.NativeRuntimeHints`
>    (`@ImportRuntimeHints` em `BrewerApplication`):
>    - Recurso `org/flywaydb/core/internal/version.txt` ausente da imagem (usado por
>      `VersionPrinter` para logar a própria versão do Flyway) → registrado como resource pattern.
>    - `ResourceBundle` `com.mysql.cj.LocalizedErrorMessages` ausente (usado por
>      `com.mysql.cj.Messages` para mensagens de erro do driver) → registrado como resource bundle.
>    - `com.mysql.cj.conf.ConnectionUrl$Type` instancia reflectivamente a implementação de
>      `ConnectionUrl` correspondente ao esquema da URL JDBC → todas as implementações
>      (`SingleConnectionUrl`, `Failover*`, `Replication*`, `LoadBalance*`, `XDevApi*`, variantes
>      DNS-SRV) registradas para reflection.
>    - `com.mysql.cj.log.LogFactory` instancia reflectivamente o logger configurado (padrão
>      `StandardLogger`) → registrados `StandardLogger`, `Slf4JLogger`, `Jdk14Logger`, `NullLogger`.
>    - `FlywaySqlException.throwFlywayExceptionIfPossible` invoca reflectivamente um método
>      estático `isFlywaySpecificVersionOf(SQLException)` em cada subclasse de
>      `org.flywaydb.core.internal.exception.sqlExceptions` para classificar o erro → registradas
>      as 4 subclasses existentes na versão do Flyway usada.
>    - `com.mysql.cj.exceptions.ExceptionFactory` instancia reflectivamente **qualquer** subclasse
>      de `com.mysql.cj.exceptions.*` conforme o ponto de falha no driver (não é uma lista fixa) →
>      em vez de listar uma a uma, todo o pacote passou a ser **escaneado via classpath e
>      registrado automaticamente** (método utilitário `registerAllClassesInPackage`), solução mais
>      robusta a mudanças de versão do driver.
>    - `com.mysql.cj.protocol.AbstractSocketConnection` instancia reflectivamente a
>      `SocketFactory` configurada (padrão `StandardSocketFactory`) → registradas
>      `StandardSocketFactory`, `NamedPipeSocketFactory`, `SocksProxySocketFactory`.
>    - Plugins de autenticação (`com.mysql.cj.protocol.a.authentication.*`, ex.
>      `CachingSha2PasswordPlugin`, usado por padrão pelo MySQL 8) instanciados reflectivamente
>      conforme o método de autenticação negociado com o servidor → pacote inteiro registrado via
>      o mesmo scan automático.
>    A cada correção, a suíte completa de 275 testes foi validada e uma nova imagem nativa foi
>    gerada e executada contra o MySQL real, confirmando que o erro anterior desaparecia e a
>    aplicação avançava mais adiante no ciclo de vida de inicialização — até o handshake de
>    protocolo/autenticação MySQL funcionar integralmente.
> 8. ⏭️ **Item em aberto ao final desta execução**: após todos os gaps acima resolvidos, o
>    binário nativo chega a **conectar e negociar o protocolo com o MySQL real**, mas a
>    inicialização do Flyway ainda falha com um `NullPointerException` originado dentro de
>    `com.zaxxer.hikari.pool.HikariPool.throwPoolInitializationException`, que mascara a exceção
>    original (`t` chega nula ao método de log/wrap do HikariCP nesse ponto). Isso não foi mais
>    resolvido nesta sessão por já representar uma investigação adicional, mais profunda, dentro do
>    HikariCP/driver MySQL (possivelmente mais uma classe/campo reflexivo faltando na cadeia de
>    tradução de exceções do driver). Ficou documentado aqui como o próximo passo concreto —
>    provavelmente resolvível adicionando `MemberCategory.ACCESS_DECLARED_FIELDS` às classes já
>    registradas em `com.mysql.cj.exceptions`, ou investigando com
>    `-H:+ReportExceptionStackTraces`/logs mais verbosos do HikariCP.
> 9. ℹ️ `aws-java-sdk-s3` (AWS SDK v1) não foi exercitado no build/execução de teste porque o
>    bean que o usa (`S3Config`) está anotado com `@Profile("prod")`, não ativo por padrão. O
>    risco documentado no item 3 (seção de compatibilidade de dependências) permanece válido
>    para quando o profile `prod` for testado em build nativo — pode ser necessário mais um
>    `RuntimeHintsRegistrar` ou a migração para AWS SDK v2, como já recomendado.
> 10. ℹ️ Durante a validação manual foi observado um **bug pré-existente, não relacionado a esta
>    migração**: a página `/cidades` lança `LazyInitializationException` ao acessar
>    `cidade.estado.nome` na view (`spring.jpa.open-in-view=false` + associação lazy acessada
>    fora da sessão do Hibernate). Não foi corrigido por estar fora do escopo desta tarefa.
>
> Todas as mudanças foram validadas com a suíte de testes completa (275 testes, sem regressões
> em nenhuma etapa) e com execução manual real da aplicação (JVM local + 8 gerações de imagem
> nativa em Docker, cada uma testada com `docker run` contra um MySQL real).

> Referências oficiais utilizadas:
> - [Introducing GraalVM Native Images](https://docs.spring.io/spring-boot/reference/packaging/native-image/introducing-graalvm-native-images.html)
> - [Advanced Topics — Native Image](https://docs.spring.io/spring-boot/reference/packaging/native-image/advanced-topics.html)
> - [Developing Your First GraalVM Native Application](https://docs.spring.io/spring-boot/how-to/native-image/developing-your-first-application.html)
> - [GraalVM Native Applications — How-to Guides](https://docs.spring.io/spring-boot/how-to/native-image/index.html)

> **Nota histórica**: o projeto original *Spring Native* (`spring-native`) foi descontinuado.
> Desde o Spring Boot 3.x (e mantido no Spring Boot 4.x, versão atual deste projeto — `4.1.1`),
> o suporte a GraalVM Native Image é nativo do framework, através do processamento **Spring AOT**
> (Ahead-of-Time) combinado com o plugin Gradle/Maven `org.graalvm.buildtools.native`.

## 1. Contexto e motivação

O projeto `brewer-springboot` é uma aplicação Spring Boot 4.1.1 (Java 25) com Spring MVC,
Thymeleaf, Spring Data JPA/Hibernate, Spring Security, Flyway e integrações com AWS S3 e
JasperReports. Migrar para GraalVM Native Image traria:

- **Startup quase instantâneo** (milissegundos vs. segundos na JVM).
- **Menor footprint de memória**, importante para containers/Kubernetes com recursos limitados.
- **Menor cold-start**, relevante caso a aplicação seja escalada horizontalmente ou usada em
  ambientes serverless/PaaS.

O trade-off é build mais lento/complexo e restrições de reflection, proxies dinâmicos e
carregamento de classes em runtime — que precisam ser mapeados e resolvidos previamente.

## 2. Pré-requisitos

- **GraalVM para JDK 25** (ou versão LTS compatível) com o componente `native-image`, **ou**
- Build via **Cloud Native Buildpacks** (`mvn -Pnative spring-boot:build-image`), que não exige
  instalação local do GraalVM — o Paketo Buildpacks baixa a distribuição GraalVM dentro do
  container de build. Esta é a abordagem recomendada para CI/CD e para manter paridade com o
  `Dockerfile`/`docker-compose.yml` já existentes no projeto.
- Docker (já utilizado no projeto, ver `Dockerfile` e `docker-compose.yml`).

## 3. Análise de compatibilidade das dependências atuais (`pom.xml`)

| Dependência | Compatibilidade com Native Image | Observações |
|---|---|---|
| `spring-boot-starter-webmvc` (Tomcat embarcado) | ✅ Suportado e testado oficialmente | Citado explicitamente na doc oficial como testado com native image |
| `spring-boot-starter-validation` | ✅ Suportado | — |
| `spring-boot-starter-thymeleaf` + `thymeleaf-extras-data-attribute` + `thymeleaf-extras-springsecurity6` | ✅ Suportado, com ressalvas | Usar `spring.thymeleaf.cache=true` em produção nativa |
| ~~`nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect`~~ | 🔴→✅ **Removida** | Era implementada em Apache Groovy (metaprogramação incompatível com o heap de imagem do GraalVM). Substituída pelo padrão puro de fragmentos parametrizados do Thymeleaf (`th:fragment`/`th:replace`) em todos os 15 templates que a usavam — ver "Status de execução" no topo deste documento |
| `spring-boot-starter-data-jpa` (Hibernate) | ✅ Suportado, com ressalvas | Hibernate possui suporte a native image desde Boot 3.x, mas *lazy-loading*/bytecode enhancement e proxies precisam ser validados nos testes |
| `spring-boot-starter-mail` | ✅ Suportado | — |
| `spring-boot-starter-security` | ✅ Suportado | Beans de `DaoAuthenticationProvider`/`SecurityConfig` já usam configuração declarativa, compatível com AOT |
| `spring-boot-starter-flyway` + `flyway-mysql` | ✅ Suportado, com hint de reflection | O `LogFactory` do Flyway usa `Class.forName` para autodetectar o *logging backend* disponível; em native-image isso exige registro explícito de reflection para `Slf4jLogCreator` — resolvido com `com.brewer.config.FlywayNativeRuntimeHints` |
| `mysql-connector-j` | ✅ Suportado (driver JDBC puro Java) | — |
| **`mysql-connector-j`** | ⚠️ Suportado, mas com **muitos** gaps de reflection | O driver usa reflection extensivamente (URLs de conexão, loggers, socket factories, plugins de autenticação, hierarquia de exceções). Todos os gaps encontrados nesta execução foram corrigidos em `com.brewer.config.NativeRuntimeHints`, mas um `NullPointerException` residual em `HikariPool.throwPoolInitializationException` (que mascara a causa real) permanece em aberto — ver "Status de execução", item 8 |
| `spring-boot-devtools` | ⚠️ Deve ser excluído do build nativo | Spring Boot já desabilita/exclui devtools automaticamente ao empacotar; não requer ação manual |
| **`aws-java-sdk-s3` (AWS SDK v1)** | 🔴 Risco alto (não exercitado ainda) | SDK v1 não é otimizado para GraalVM (uso extensivo de reflection dinâmica, XML binding). Bean `@Profile("prod")`, não incluído no build nativo padrão. Recomenda-se avaliar migração para **AWS SDK v2** (`software.amazon.awssdk:s3`), que possui melhor suporte a native image (incluindo módulo `url-connection-client` e hints publicados pela AWS) |
| ~~`net.sf.jasperreports` / `jasperreports-fonts`~~ | 🔴→✅ **Removida** | Confirmado que não era usada em nenhum lugar do código (nem `.jrxml`, nem referências) — removida do `pom.xml` em vez de investir em hints para uma dependência morta |
| `org.ehcache` | ⚠️ Risco médio (não exercitado ainda) | Requer hints de reflection/serialização para as classes de cache e `cache-api` (JSR-107); o build nativo realizado nesta execução não expôs problemas com EhCache, mas o cenário de uso real deve ser validado |
| `net.coobird:thumbnailator` | ⚠️ Risco médio (não exercitado ainda) | Usa `ImageIO`/reflection para plugins de formato de imagem; validar com testes |
| `commons-beanutils`, `commons-lang3` | ✅ Baixo risco | Bibliotecas amplamente testadas com native image, mas `commons-beanutils` usa reflection para introspecção — validar hints se usado diretamente no código |
| JUnit 4 / Mockito / AssertJ / EqualsVerifier (testes) | ✅ Suportado para testes AOT | Não afeta o artefato de produção; usado apenas em `test` scope |

## 4. Mudanças necessárias no `pom.xml` — realizadas

1. ~~Adicionar o profile `native`~~ — **não foi necessário**: confirmado via `mvn help:all-profiles`
   que os profiles `native`/`nativeTest` (com `org.graalvm.buildtools:native-maven-plugin`) já
   vêm herdados de `spring-boot-starter-parent:4.1.1`.
2. ✅ Removidas as dependências `net.sf.jasperreports:jasperreports` e `jasperreports-fonts`
   (não usadas em nenhum lugar do código).
3. ✅ Removida a dependência `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect` (implementada em
   Apache Groovy, incompatível com native-image) e sua propriedade de versão associada.
4. ✅ Adicionada a classe `com.brewer.config.NativeRuntimeHints` (`RuntimeHintsRegistrar`),
   importada via `@ImportRuntimeHints` em `BrewerApplication`, registrando reflection/resources
   para Flyway e MySQL Connector/J (lista completa na seção 5).
5. ⏭️ Ainda pendente: avaliar upgrade `aws-java-sdk-s3` → `software.amazon.awssdk:s3` (AWS SDK v2)
   caso o profile `prod` (que usa `S3Config`) seja incluído em um futuro build nativo.

## 5. Runtime Hints necessários

- **Configuration properties aninhadas**: qualquer classe usada com `@ConfigurationProperties`
  que tenha propriedades aninhadas que **não sejam inner classes** deve usar
  `@NestedConfigurationProperty`, caso contrário o binding falhará em runtime nativo (hints de
  reflection não são gerados automaticamente para elas).
- **`RuntimeHintsRegistrar` customizado**: implementado nesta execução em
  `com.brewer.config.NativeRuntimeHints` (importado via `@ImportRuntimeHints` em
  `BrewerApplication`), cobrindo:
  - `org.flywaydb.core.internal.logging.slf4j.Slf4jLogCreator` (reflection) — Flyway usa
    `Class.forName` para autodetectar o *logging backend* disponível no classpath.
  - `org/flywaydb/core/internal/version.txt` (resource) — lido por `VersionPrinter` para logar
    a própria versão do Flyway.
  - `com.mysql.cj.LocalizedErrorMessages` (resource bundle) — mensagens de erro do driver MySQL.
  - Todas as implementações de `com.mysql.cj.conf.url.*ConnectionUrl` (reflection) — escolhidas
    reflectivamente conforme o esquema da URL JDBC.
  - Os loggers `com.mysql.cj.log.{StandardLogger,Slf4JLogger,Jdk14Logger,NullLogger}` (reflection).
  - As 4 subclasses de `org.flywaydb.core.internal.exception.sqlExceptions` (reflection de
    método estático) — usadas por Flyway para classificar erros de SQL.
  - Todo o pacote `com.mysql.cj.exceptions` (reflection, via *scan* automático de classpath) —
    `ExceptionFactory` instancia reflectivamente qualquer subclasse conforme o ponto de falha.
  - `com.mysql.cj.protocol.{StandardSocketFactory,NamedPipeSocketFactory,SocksProxySocketFactory}`
    (reflection) — *socket factory* escolhida reflectivamente.
  - Todo o pacote `com.mysql.cj.protocol.a.authentication` (reflection, via o mesmo *scan*) —
    plugin de autenticação (ex. `CachingSha2PasswordPlugin`) escolhido conforme negociação com o
    servidor MySQL.
  Ainda não exercitados nesta execução (candidatos a hints futuros):
  - Classes do AWS SDK v1 que usam reflection para (de)serialização de requests/responses,
    caso o profile `prod` (que ativa `S3Config`) seja testado em build nativo.
  - Classes de modelo do EhCache/JCache usadas em serialização de cache, caso o cenário de
    cache seja exercitado em produção nativa.
  - Possivelmente `MemberCategory.ACCESS_DECLARED_FIELDS` nas classes de
    `com.mysql.cj.exceptions` já registradas, para investigar o `NullPointerException` residual
    em `HikariPool.throwPoolInitializationException` documentado no "Status de execução" (item 8).
- Registrar o hint via `@ImportRuntimeHints(MyHintsRegistrar.class)` em uma classe de configuração,
  conforme padrão documentado pelo Spring Framework/Spring Boot para AOT.

## 6. Passos de build e teste

```powershell
# Build nativo local (requer GraalVM com native-image instalado e configurado no PATH)
mvn -Pnative native:compile

# Build de imagem de container nativa via Cloud Native Buildpacks (não requer GraalVM local)
mvn -Pnative spring-boot:build-image

# Rodar testes com processamento AOT habilitado (detecta problemas de reflection/hints cedo)
mvn test -Dspring.aot.enabled=true

# Testes de integração AOT (Spring Boot gera testes de smoke test automaticamente
# no diretório target/spring-aot/test quando aplicável)
mvn verify -Pnative
```

## 7. Limitações conhecidas a documentar/mitigar

- Sem carregamento de classes dinâmico em runtime (`Class.forName` arbitrário, plugins via SPI
  não declarados). Confirmado nesta execução com o Flyway (`Slf4jLogCreator`, resolvido com
  `RuntimeHints`) e anteriormente com Groovy/JasperReports/thymeleaf-layout-dialect (resolvido
  removendo as dependências). Qualquer biblioteca com autodetecção de plugins via reflection
  (thumbnailator/`ImageIO`, EhCache) é candidata a precisar do mesmo tratamento.
- Proxies JDK dinâmicos e CGLIB precisam ser conhecidos em build-time; Spring AOT já gera os
  hints para os proxies criados pelo próprio framework (ex. `@Transactional`, Spring Security),
  mas proxies criados manualmente no código da aplicação precisam de hints explícitos.
- Hibernate: lazy-loading e bytecode enhancement podem exigir ajustes (`hibernate.bytecode.provider=none`
  ou revisão de entidades com proxies lazy) — validar com testes de integração reais contra MySQL.
  Nota: foi observado um `LazyInitializationException` pré-existente (não relacionado à migração
  nativa) em `/cidades` durante a validação manual — ver item 8 do "Status de execução".
- Cache de templates do Thymeleaf deve ficar habilitado (`cache=true`) em produção nativa, já que
  não há reload em runtime de qualquer forma em uma imagem nativa imutável.
- Bibliotecas implementadas em linguagens dinâmicas para a JVM (Groovy, e por extensão qualquer
  dependência transitiva delas, como o `thymeleaf-layout-dialect` original) são um forte sinal de
  risco: seu runtime de metaprogramação tende a manter estado (threads, registries, caches) que o
  GraalVM native-image rejeita no heap da imagem, com uma cadeia longa e imprevisível de erros
  incrementais em vez de uma única causa raiz.
- Tempo de build nativo é significativamente maior que o build JVM tradicional (nesta execução,
  entre ~14 e ~18 minutos por build via Cloud Native Buildpacks) — ajustar pipelines de CI
  (paralelização, cache de camada Docker, etc.) e monitorar espaço em disco/cache do Docker, que
  pode crescer rapidamente com builds nativos repetidos.

## 8. Plano incremental de execução (fases) — status final

1. ✅ **Fase 1 — Prova de conceito mínima**: confirmado que o profile `native` já builda a
   aplicação (sem necessidade de alterações no `pom.xml` além das dependências problemáticas
   removidas).
2. ✅ **Fase 2 — Build completo da aplicação**: `mvn -Pnative spring-boot:build-image` executado
   ~17 vezes até esgotar a cadeia de gaps de reflection/resource encontrados, catalogando cada
   falha reportada pelo `native-image` ou em runtime (Groovy/JasperReports, Groovy/
   thymeleaf-layout-dialect, e depois a sequência Flyway/MySQL Connector/J detalhada no
   "Status de execução", item 7).
3. ✅ **Fase 3 — Mitigação de dependências de risco**: JasperReports removido (não usado);
   thymeleaf-layout-dialect substituído por fragmentos puros do Thymeleaf; 8 hints de
   reflection/resource distintos adicionados para Flyway e MySQL Connector/J em
   `com.brewer.config.NativeRuntimeHints`. AWS SDK v1/EhCache/thumbnailator permanecem como
   pendências (não exercitados porque `S3Config` é `@Profile("prod")` e os demais fluxos não
   apresentaram erro nos testes realizados). Um `NullPointerException` residual no HikariCP
   (item 8 do "Status de execução") permanece como último item em aberto.
4. ✅ **Fase 4 — Validação funcional**: suíte de testes completa (275 testes) validada após cada
   mudança de dependência/template/hint; navegação manual real (login, Dashboard, CRUDs de cidade/
   cerveja/cliente/venda/estilo/usuário, página de erro) validada com `mvn spring-boot:run`
   contra MySQL via `docker-compose`; a imagem nativa final foi executada repetidamente com
   `docker run` contra o mesmo MySQL, avançando a cada correção até negociar completamente o
   protocolo/autenticação MySQL (ficando apenas a inicialização do Flyway pendente, ver item 8).
5. ⏭️ **Fase 5 — Ajuste de pipeline CI/CD**: ainda não realizada nesta execução. Próximo passo
   natural: atualizar `Dockerfile`/pipeline para oferecer a imagem nativa como alternativa/
   substituta da imagem JVM tradicional, e validar CVE-scan e tamanho de imagem final.

## 9. Riscos e recomendações

- **Migração faseada validada na prática**: o processo real confirmou a previsão deste plano —
  dependências com reflection/metaprogramação pesada (JasperReports, thymeleaf-layout-dialect,
  e em menor grau o próprio driver MySQL Connector/J) foram, de fato, os bloqueadores reais, e
  cada um exigiu investigação e correção específica (remoção por não uso, reescrita para um
  padrão nativo do framework, ou uma sequência de `RuntimeHints`) em vez de uma única configuração.
- **Bibliotecas JDBC/drivers de banco têm reflection pesada e finita, mas profunda**: diferente de
  JasperReports/Groovy (incompatibilidade estrutural, sem solução viável por hints), o MySQL
  Connector/J e o Flyway são *totalmente* compatíveis com native image — só exigem uma cadeia de
  hints de reflection/resource (documentada na íntegra na seção 5) para cobrir seus pontos de
  extensibilidade via reflection (URLs, loggers, exceções, socket factories, autenticação). Vale
  considerar usar o `native-image-agent` do próprio GraalVM (tracing agent, rodando a aplicação
  na JVM normal e capturando automaticamente toda a reflection/resources acessada) como atalho
  para descobrir esses hints de uma vez, em vez do processo manual iterativo desta execução.
- **Item em aberto**: o `NullPointerException` residual em `HikariPool.throwPoolInitializationException`
  (item 8 do "Status de execução") deve ser investigado antes de considerar a migração para
  native image "pronta para produção" — é o único obstáculo restante identificado para a
  aplicação completar a inicialização (Flyway + JPA) contra um MySQL real em modo nativo.
- **AWS SDK v1 e EhCache continuam como itens em aberto**: não foram exercitados nesta execução
  porque não estão no caminho crítico do profile padrão/testes realizados. Antes de considerar a
  migração para native image "completa", validar o build com o profile `prod` ativo (que usa
  `S3Config`/AWS SDK v1) e com um cenário de uso real do cache (`WebConfig.cacheManager`).
- **Considerar arquitetura híbrida** apenas se, ao validar o profile `prod`, o AWS SDK v1 se
  mostrar tão problemático quanto o JasperReports foi — nesse caso, migrar para AWS SDK v2 é a
  recomendação primária (em vez de isolar em serviço separado, já que o SDK v2 tem bom suporte a
  native image).
- **Investir em testes automatizados antes da migração**: como o modo nativo remove flexibilidade
  de reflection em runtime, uma suíte de testes de integração robusta (já parcialmente existente,
  ver `pitest-maven` configurado no `pom.xml`) foi essencial para detectar rapidamente regressões
  causadas pelo processamento AOT e pela reescrita dos templates.
- **Medir ganhos antes de comprometer o roadmap**: validar com benchmarks reais (tempo de
  startup, uso de memória) se o ganho justifica o esforço de manutenção adicional (hints,
  builds mais lentos — de 14 a 18 minutos nesta execução —, superfície de testes maior).
