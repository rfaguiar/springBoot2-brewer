# Plano: Observabilidade OpenTelemetry + SigNoz (brewer-springboot)

## Contexto levantado
- Spring Boot 4.1.1, Java 25, Maven. Sem actuator/micrometer/tracing hoje.
- Logging: logback-spring.xml (base.xml do Spring Boot). Sem AOP/interceptors.
- Docker: multi-stage (maven build -> eclipse-temurin:25-jre-alpine), non-root user `spring`,
  `ENV JAVA_OPTS=""`, docker-entrypoint.sh já executa `exec java $JAVA_OPTS -jar /app/app.jar`
  (pronto para -javaagent sem alterar o entrypoint).
- docker-compose.yml atual: serviços `mysql` (healthcheck) + `app` (depends_on mysql healthy),
  volume nomeado para dados do mysql, env vars via `environment:`.
- Pacotes chave para spans de negócio: `com.brewer.service.CadastroVendaService`,
  `com.brewer.service.event.venda.VendaListener`, `com.brewer.controller.VendasController`,
  `com.brewer.controller.FotosController`, `com.brewer.mail.Mailer`.

## Decisões (via perguntas ao usuário)
- Backend/UI: **SigNoz (all-in-one)** — logs + métricas + traces correlacionados numa única UI.
- Instrumentação: **ambos** — OTel Java Agent (auto-instrumentação zero-code: HTTP, JDBC, JPA, logs)
  + SDK/anotações manuais (`@WithSpan`) para spans customizados do fluxo de vendas.
- Collector: **serviço centralizado único** (não sidecar por serviço).
- Persistência: **volumes Docker** para dados de observabilidade.
- Escopo de negócio: incluir spans customizados no fluxo de Vendas
  (`VendasController` / `CadastroVendaService` / `VendaListener`).
- Instalação do SigNoz: método oficial atual mudou — não há mais docker-compose estático
  distribuído; agora usa **Foundry** (`foundryctl`) que gera um stack Compose separado
  (`pours/deployment/compose.yaml`). Usuário escolheu **usar Foundry (oficial) com stack
  separado do SigNoz + rede Docker externa compartilhada** conectando ao docker-compose.yml
  da aplicação (em vez de escrever manualmente os serviços legados do SigNoz).
- Ambiente: usuário roda **Docker Desktop no Windows**. Risco documentado oficialmente pelo
  SigNoz: `clickhouse-keeper` pode travar com segfault (exit 139) sob a virtualização do
  Docker Desktop no Windows; a doc recomenda Docker Engine nativo em WSL2. Usuário optou por
  seguir mesmo assim com Docker Desktop — **tratar como risco conhecido documentado no plano**,
  com mitigação sugerida (mover para WSL2 nativo se houver crash loop).

## Plano

**Fase 1 — Provisionar stack SigNoz via Foundry (paralelo às demais fases, sem dependência do código Java)**

> **Status: ✅ IMPLEMENTADA E VALIDADA em 2026-09-24.** Ver seção
> [Fase 1 — Registro de implementação](#fase-1--registro-de-implementação) no final deste
> documento com os detalhes e resultados dos testes.

1. Instalar `foundryctl` (script oficial) e criar `casting.yaml` na raiz do projeto (ou em
   `observability/casting.yaml`) com `flavor: compose`, `mode: docker`.
2. Rodar `foundryctl gauge` (valida pré-requisitos Docker) e `foundryctl forge` (gera os
   arquivos em `pours/deployment/`) — **sem** `cast` ainda, para primeiro inspecionar o
   compose gerado.
3. Inspecionar `pours/deployment/compose.yaml` gerado para identificar o nome exato do
   serviço/container do `signoz-otel-collector` (ingester OTLP, portas 4317/4318) — este nome
   será usado como host de destino OTLP a partir do app.
4. Criar rede Docker externa compartilhada (ex.: `docker network create brewer-observability`)
   e adicionar via **patch** do Foundry (`spec.patches`, conforme documentado — Foundry não
   modela redes nativamente) para anexar o serviço otel-collector/ingester a essa rede externa.
5. Rodar `foundryctl cast -f casting.yaml` para subir o stack SigNoz.
6. Documentar no Readme/docs o comando para subir o SigNoz (`foundryctl cast`) como pré-requisito
   antes de `docker compose up` da aplicação.
7. **Risco documentado**: no Windows com Docker Desktop, `clickhouse-keeper` pode entrar em
   crash loop (segfault). Se ocorrer, mitigação = rodar Docker Engine nativo dentro do WSL2
   (não Docker Desktop) — anotar isso claramente na documentação/README.

**Fase 2 — Conectar docker-compose.yml da aplicação à rede do SigNoz** (*depende da Fase 1, passo 4*)

> **Status: ✅ IMPLEMENTADA E VALIDADA em 2026-09-24** (sem o `JAVA_OPTS`/agent, adiado para a
> Fase 3 — decisão do usuário). Ver seção
> [Fase 2 — Registro de implementação](#fase-2--registro-de-implementação) no final deste
> documento com os detalhes e resultados dos testes.

1. Em [docker-compose.yml](../../docker-compose.yml): declarar a rede externa
   `brewer-observability` (`external: true`) e anexar o serviço `app` a ela, mantendo a rede
   default (para `mysql`).
2. Adicionar variáveis de ambiente no serviço `app`:
   - `OTEL_EXPORTER_OTLP_ENDPOINT=http://<nome-serviço-ingester-signoz>:4317`
   - `OTEL_EXPORTER_OTLP_PROTOCOL=grpc`
   - `OTEL_SERVICE_NAME=brewer-springboot`
   - `OTEL_RESOURCE_ATTRIBUTES=service.namespace=brewer,deployment.environment=docker`
   - `OTEL_TRACES_EXPORTER=otlp`, `OTEL_METRICS_EXPORTER=otlp`, `OTEL_LOGS_EXPORTER=otlp`
   - `OTEL_INSTRUMENTATION_LOGBACK_APPENDER_ENABLED=true` (correlação de logs)
   - `JAVA_OPTS=-javaagent:/app/opentelemetry-javaagent.jar` (mantém `docker-entrypoint.sh`
     inalterado, que já propaga `$JAVA_OPTS`)
   - `MANAGEMENT_OTLP_METRICS_EXPORT_URL=http://<nome-serviço-ingester-signoz>:4318/v1/metrics`
     (para o registry Micrometer OTLP — protocolo HTTP/protobuf na porta 4318)

**Fase 3 — Empacotar o OpenTelemetry Java Agent na imagem** (*paralelo com Fase 4, depende só do Dockerfile*)

> **Status: ✅ IMPLEMENTADA E VALIDADA em 2026-09-24.** Ver seção
> [Fase 3 — Registro de implementação](#fase-3--registro-de-implementação) no final deste
> documento com os detalhes e resultados dos testes.

1. Em [Dockerfile](../../Dockerfile): adicionar etapa (no stage final, antes de `USER spring`) para
   baixar o `opentelemetry-javaagent.jar` (release oficial do repositório
   `open-telemetry/opentelemetry-java-instrumentation`, versão fixada) para `/app/opentelemetry-javaagent.jar`,
   garantindo permissão de leitura pelo usuário `spring`.
2. Confirmar que `ENV JAVA_OPTS=""` continua servindo de default local (sem agent) e que o
   valor real do agent só é setado via `docker-compose.yml` (Fase 2), preservando execução
   local (`mvn spring-boot:run`) sem overhead do agent.

**Fase 4 — Dependências e config Spring para métricas/health (pilar métricas + health)** (*paralelo com Fase 3*)
1. [pom.xml](../../pom.xml): adicionar
   - `org.springframework.boot:spring-boot-starter-actuator`
   - `io.micrometer:micrometer-registry-otlp`
   - `io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations` (para `@WithSpan`
     manual; funciona como no-op sem o agent, e é capturado pelo agent quando presente)
2. [application.properties](../../src/main/resources/application.properties): adicionar
   - `management.endpoints.web.exposure.include=health,info,metrics`
   - `management.endpoint.health.probes.enabled=true`
   - `management.otlp.metrics.export.url=${MANAGEMENT_OTLP_METRICS_EXPORT_URL:}` (verificar em
     tempo de implementação se a auto-configuração do Micrometer OTLP no Spring Boot 4.1.1
     usa exatamente essa chave de propriedade; ajustar se o nome mudou)
3. (Opcional/nice-to-have) [logback-spring.xml](../../src/main/resources/logback-spring.xml):
   incluir `%X{trace_id}` / `%X{span_id}` no pattern de log para correlação legível no console
   local (o agente já injeta esses valores no MDC).

**Fase 5 — Spans customizados do fluxo de Vendas** (*depende da Fase 4, passo 1 — precisa da dependência de anotações*)
1. [CadastroVendaService.java](../../src/main/java/com/brewer/service/CadastroVendaService.java):
   anotar métodos de emissão/cancelamento de venda com `@WithSpan`, adicionar atributos
   relevantes via `Span.current().setAttribute(...)` (ex.: id da venda, valor total, status).
2. [VendaListener.java](../../src/main/java/com/brewer/service/event/venda/VendaListener.java):
   `@WithSpan` no listener que baixa estoque ao emitir venda.
3. [VendasController.java](../../src/main/java/com/brewer/controller/VendasController.java): avaliar
   pontos de entrada críticos (emitir, cancelar, gerar PDF) para `@WithSpan` adicional, caso
   não já cobertos suficientemente pela auto-instrumentação HTTP do agent.
4. [Mailer.java](../../src/main/java/com/brewer/mail/Mailer.java): `@WithSpan` no envio de e-mail
   com PDF de confirmação de venda.
5. [FotosController.java](../../src/main/java/com/brewer/controller/FotosController.java): `@WithSpan`
   em upload/download de fotos (fluxo com FotoStorageLocal/S3).

**Fase 6 — Documentação (opcional)**
1. Adicionar `docs/11-observabilidade.md` seguindo o padrão numérico existente, descrevendo:
   arquitetura (agent + SigNoz via Foundry), como subir localmente, portas, variáveis de
   ambiente, e o risco/mitigação do ClickHouse Keeper no Windows.
2. Atualizar [08-execucao-local-e-docker.md](../08-execucao-local-e-docker.md) com o passo
   extra de subir o SigNoz antes do `docker compose up` da aplicação.

## Relevant files
- `docker-compose.yml` — rede externa + env vars OTEL/JAVA_OPTS no serviço `app`
- `Dockerfile` — download do opentelemetry-javaagent.jar no stage final
- `pom.xml` — actuator, micrometer-registry-otlp, opentelemetry-instrumentation-annotations
- `src/main/resources/application.properties` — exposição actuator + otlp metrics export
- `src/main/resources/logback-spring.xml` — pattern com trace_id/span_id (opcional)
- `src/main/java/com/brewer/service/CadastroVendaService.java` — `@WithSpan`
- `src/main/java/com/brewer/service/event/venda/VendaListener.java` — `@WithSpan`
- `src/main/java/com/brewer/controller/VendasController.java` — `@WithSpan` pontual
- `src/main/java/com/brewer/mail/Mailer.java` — `@WithSpan`
- `src/main/java/com/brewer/controller/FotosController.java` — `@WithSpan`
- Novo: `casting.yaml` (config Foundry para o stack SigNoz)
- Novo (opcional): `docs/11-observabilidade.md`

## Verification
1. `docker network create brewer-observability` + `foundryctl cast -f casting.yaml` sobe SigNoz
   sem crash loop de `clickhouse-keeper` (checar `docker ps`/`docker logs`).
2. `docker compose up --build -d` sobe app+mysql conectados à rede externa; checar
   `docker logs brewer-app` por linha de inicialização do OTel Java Agent (sem erros de conexão
   OTLP com o coletor).
3. Acessar a UI do SigNoz (`http://localhost:8080`) e confirmar: serviço `brewer-springboot`
   aparece na lista de serviços, com traces de requisições HTTP, métricas (JVM, HTTP,
   customizadas) e logs correlacionados por trace_id.
4. Exercitar fluxo de venda (criar cliente/cerveja, montar carrinho, emitir venda) via UI da
   aplicação e verificar no SigNoz um trace com spans customizados (`CadastroVendaService`,
   `VendaListener`, `Mailer`) aninhados sob o span HTTP raiz.
5. Rodar suíte de testes existente (`mvn test`) para garantir que anotações `@WithSpan`
   não quebram testes unitários (anotação é inerte sem o agent no classpath de teste).
6. Validar que execução local sem Docker (`mvn spring-boot:run`, sem `JAVA_OPTS`) continua
   funcionando normalmente (agent é opcional/apenas via docker-compose).

## Riscos e Considerações
- ClickHouse Keeper + Docker Desktop no Windows: risco de crash loop documentado oficialmente
  pelo SigNoz; usuário optou por seguir mesmo assim. Mitigação: migrar para Docker Engine
  nativo em WSL2 se instabilidade ocorrer.
- Nome exato do serviço otel-collector/ingester gerado pelo Foundry só é conhecido após rodar
  `foundryctl forge` (passo 3 da Fase 1) — necessário para preencher `OTEL_EXPORTER_OTLP_ENDPOINT`
  corretamente na Fase 2.
- Verificar em tempo de implementação se `management.otlp.metrics.export.url` é a chave correta
  de auto-configuração no Spring Boot 4.1.1 (stack muito recente); caso não exista, registrar
  bean `OtlpMeterRegistry` manualmente.
- Versão do `opentelemetry-javaagent.jar` deve ser fixada (não usar `latest` em produção) para
  builds reprodutíveis do Dockerfile.

## Fase 1 — Registro de implementação

**Concluída e validada em 2026-09-24.**

### O que foi feito
1. `foundryctl` instalado em `.tools/foundry_windows_amd64/bin/foundryctl.exe` (gitignored).
2. Criado [observability/casting.yaml](../../observability/casting.yaml) com `flavor: compose`,
   `mode: docker`, e um patch JSON (RFC 6902) em `deployment/compose.yaml` que:
   - declara a rede externa `brewer-observability` (`external: true`);
   - anexa o serviço `ingester` (coletor OTLP gerado pelo Foundry) a essa rede.
3. Rede Docker externa criada manualmente: `docker network create brewer-observability`.
4. Stack subido com `foundryctl cast -f casting.yaml` (redirecionando saída para arquivo, ver
   bug de terminal abaixo). Serviços gerados: `signoz-telemetrystore-clickhouse-0-0`,
   `signoz-metastore-postgres-0` (metastore), `signoz-telemetrykeeper-clickhousekeeper-0`,
   `signoz-signoz-0` (UI/API, porta 8080), `signoz-telemetrystore-migrator` (job de migração,
   one-shot), `signoz-ingester-1` (coletor OTLP, portas 4317/4318, anexado também à rede
   `brewer-observability`). Todos os volumes de dados são nomeados/persistentes por padrão do
   Foundry (atende à decisão de persistência).
5. Conta admin inicial criada via UI (`http://localhost:8080/signup`) para bootstrapping do
   SigNoz — necessária para o funcionamento do coletor (ver bug abaixo). Credencial de
   desenvolvimento local: `admin@brewer.local` / `Brewer@Obs2026!` (apenas ambiente local,
   sem dados sensíveis).

### Bugs/obstáculos encontrados e resolvidos
1. **Patch de rede falhava no Windows**: `target: "deployment/compose.yaml"` (barra normal)
   retornava `"patch target ... did not match any generated material"`. Causa: o foundryctl
   no Windows casa o target do patch usando separador de caminho nativo do SO. Correção: usar
   barra invertida escapada — `target: "deployment\\compose.yaml"`.
2. **Comando paralelo matou o `cast` em andamento**: ao rodar um segundo comando no mesmo
   terminal enquanto `foundryctl cast` ainda baixava imagens, o processo foi encerrado
   (`ExitCode -1073741510` = interrupção). Correção: nunca reusar o mesmo terminal para outro
   comando enquanto um `cast`/`docker compose` está em execução; redirecionar a saída do `cast`
   para arquivo (`*> cast-output.log`) evita também problemas de renderização da barra de
   progresso no PowerShell.
3. **OTLP receiver (4317/4318) não respondia (`Connection refused` via rede, `Empty reply`
   via host) mesmo com os containers "healthy"**: o `ingester` sobe inicialmente com uma
   config mínima (só extensions `pprof`/`health_check`) e depende do backend `signoz-signoz-0`
   entregar a config real (com os receivers OTLP) via protocolo OpAMP. Isso só acontece depois
   que existe uma organização no SigNoz. Nos logs do `signoz-signoz-0` aparecia
   `"failed to find or create agent" ... "cannot create agent without orgId"` repetidamente.
   Correção: completar o signup inicial em `http://localhost:8080` (cria a primeira
   organização/conta admin). Após isso, os logs do `ingester` mostraram
   `"Config has changed, reloading"` seguido de `"Starting GRPC server ... endpoint [::]:4317"`
   e `"Starting HTTP server ... endpoint [::]:4318"`.
4. **ClickHouse Keeper no Docker Desktop/Windows**: o risco documentado oficialmente pelo
   SigNoz (crash loop/segfault) **não se materializou** — o container rodou de forma estável
   por mais de 3 horas durante os testes. Risco permanece documentado para monitoramento, mas
   não bloqueou a Fase 1.

### Testes de validação executados (todos com resultado ✅)
1. `docker ps -a` — todos os containers de longa duração em estado `healthy`; jobs one-shot
   (`clickhouse-user-scripts`, `telemetrystore-migrator`) terminaram com `Exited (0)`.
2. Rede externa `brewer-observability` confirmada via `docker network inspect`: contém o
   container `signoz-ingester-1` com IP dedicado nessa rede.
3. Conectividade cross-network simulando o container `app` (que será anexado à mesma rede
   externa na Fase 2): `docker run --rm --network brewer-observability curlimages/curl ...`
   contra `http://ingester:4318/v1/traces` → **HTTP 200**; TCP connect em `ingester:4317`
   (gRPC) → conexão estabelecida com sucesso.
4. UI do SigNoz acessível em `http://localhost:8080`, conta criada, workspace ativo
   (`"Your workspace is ready"`).
5. Estabilidade: containers seguiram saudáveis por 3h+ sem reinícios/crash loops.

### Pendências para as próximas fases
- O nome do serviço/hostname a usar em `OTEL_EXPORTER_OTLP_ENDPOINT` na Fase 2 é `ingester`
  (nome do serviço Compose; funciona como hostname DNS dentro da rede `brewer-observability`) —
  não é necessário usar o alias `signoz-ingester` (esse alias só vale dentro da rede interna
  `signoz-network`).
- Ao reiniciar a máquina/Docker, os containers do SigNoz precisam ser religados
  (`cd observability && ..\.tools\foundry_windows_amd64\bin\foundryctl.exe cast -f casting.yaml`,
  ou simplesmente `docker compose -f observability/pours/deployment/compose.yaml up -d` se os
  arquivos já tiverem sido gerados).

## Fase 2 — Registro de implementação

**Concluída e validada em 2026-09-24.** Escopo: apenas rede + variáveis de ambiente do
`docker-compose.yml` da aplicação. **`JAVA_OPTS`/`-javaagent` foi deliberadamente omitido**
nesta fase (decisão do usuário) porque o `opentelemetry-javaagent.jar` só é empacotado na
Fase 3 (Dockerfile), que não fazia parte do escopo pedido — setar o agente sem o jar presente
quebraria a subida do container `app`.

### O que foi feito
1. Rede externa `brewer-observability` criada manualmente (já existia da Fase 1, recriada
   nesta sessão com `docker network create brewer-observability`).
2. Em [docker-compose.yml](../../docker-compose.yml), serviço `app`:
   - adicionada seção `networks:` com `default` (mantém acesso ao `mysql`) + `brewer-observability`
     (acesso ao `ingester` do SigNoz);
   - adicionado bloco `networks:` de topo declarando `brewer-observability` como `external: true`;
   - adicionadas variáveis: `OTEL_EXPORTER_OTLP_ENDPOINT=http://ingester:4317`,
     `OTEL_EXPORTER_OTLP_PROTOCOL=grpc`, `OTEL_SERVICE_NAME=brewer-springboot`,
     `OTEL_RESOURCE_ATTRIBUTES=service.namespace=brewer,deployment.environment=docker`,
     `OTEL_TRACES_EXPORTER=otlp`, `OTEL_METRICS_EXPORTER=otlp`, `OTEL_LOGS_EXPORTER=otlp`,
     `OTEL_INSTRUMENTATION_LOGBACK_APPENDER_ENABLED=true`,
     `MANAGEMENT_OTLP_METRICS_EXPORT_URL=http://ingester:4318/v1/metrics`;
   - **não** foi adicionado `JAVA_OPTS=-javaagent:...` (ver acima).
3. Publicação de porta do serviço `app` alterada de `8080:8080` para **`8081:8080`** no host
   (motivo: conflito de porta — ver bug 2 abaixo) e `BREWER_FOTO_STORAGE_LOCAL_URL_BASE`
   atualizada para `http://localhost:8081/fotos/` para continuar consistente com a nova porta
   publicada.
4. Criado [.gitattributes](../../.gitattributes) na raiz forçando `eol=lf` (geral e para
   `*.sh`) e normalizado [docker-entrypoint.sh](../../docker-entrypoint.sh) para terminações de
   linha LF (ver bug 1 abaixo).
5. Stack completo testado de ponta a ponta: `docker network create` → `foundryctl cast`
   (SigNoz) → `docker compose up --build -d` (mysql + app), ambos os stacks simultâneos.

### Bugs/obstáculos encontrados e resolvidos
1. **`brewer-app` em crash loop (`Restarting (255)`), log `exec /app/docker-entrypoint.sh: no
   such file or directory`**: o arquivo estava com terminações de linha CRLF no checkout local
   (Windows/`core.autocrlf`), então o shebang `#!/bin/sh\r` não é reconhecido pelo kernel Linux
   do container (procura um interpretador chamado `/bin/sh\r`, que não existe — daí o erro
   enganoso de "arquivo não encontrado" em vez de erro de sintaxe). Correção: normalizado o
   arquivo para LF e adicionado `.gitattributes` (`* text=auto eol=lf`, `*.sh text eol=lf`)
   para evitar recorrência em outras máquinas Windows.
2. **Conflito de porta 8080 no host**: o serviço `signoz-signoz-0` (UI/API do SigNoz, gerado
   pelo Foundry na Fase 1) já publica `8080:8080` no host. O `docker-compose.yml` da aplicação
   também publicava `8080:8080`, o que impediria os dois stacks de rodarem simultaneamente
   (exigido para validar a Fase 2 de ponta a ponta). Correção: publicado o serviço `app` em
   `8081:8080` e ajustada a env var `BREWER_FOTO_STORAGE_LOCAL_URL_BASE` de acordo.

### Testes de validação executados (todos com resultado ✅)
1. `docker compose up --build -d`: build da imagem e subida de `brewer-mysql` (healthy) e
   `brewer-app` sem crash loop, com logs completos de migração Flyway e
   `"Started BrewerApplication in 20.541 seconds"`.
2. `docker inspect brewer-app` confirmou o container conectado a **duas redes**:
   `springboot2-brewer_default` (acesso ao `mysql`) e `brewer-observability` (acesso ao
   `ingester` do SigNoz).
3. Requisição HTTP `http://localhost:8081/` → **302** (redirect de login, comportamento normal
   da aplicação); simultaneamente `http://localhost:8080/api/v1/health` (UI do SigNoz) →
   **200** — confirma ausência de conflito de porta entre os dois stacks.
4. De dentro do container `brewer-app` (`docker exec`): `getent hosts ingester` resolveu para
   `172.19.0.2` (IP da rede `brewer-observability`); `nc -zv ingester 4317` → **conexão aberta**
   (porta gRPC); requisição HTTP OTLP (`POST /v1/traces` com corpo `{}`) para
   `http://ingester:4318/v1/traces` → resposta **`{"partialSuccess":{}}`**, confirmando que o
   coletor aceita e processa requisições OTLP vindas do container da aplicação.
5. `docker exec brewer-app env | grep OTEL_` confirmou todas as 8 variáveis OTEL e a
   `MANAGEMENT_OTLP_METRICS_EXPORT_URL` presentes e com os valores corretos dentro do container
   em execução.

### Pendências para a próxima fase (Fase 3)
- O app ainda **não emite telemetria de fato**: as variáveis `OTEL_*` só têm efeito quando o
  OpenTelemetry Java Agent está no classpath (`-javaagent:...`), o que só será adicionado na
  Fase 3 (empacotamento do jar no Dockerfile) + no `JAVA_OPTS` do `docker-compose.yml`.
- Ao implementar a Fase 3, adicionar `JAVA_OPTS: -javaagent:/app/opentelemetry-javaagent.jar`
  ao serviço `app` no `docker-compose.yml` (a infraestrutura de rede/env vars já está pronta) e
  então revalidar via UI do SigNoz (`http://localhost:8080`) que o serviço `brewer-springboot`
  aparece com traces reais.
- Porta do serviço `app` agora é **8081** (não mais 8080) enquanto o stack do SigNoz estiver
  ativo simultaneamente — atualizar documentação de execução local/Docker
  ([08-execucao-local-e-docker.md](../08-execucao-local-e-docker.md)) na Fase 6.

## Fase 3 — Registro de implementação

**Concluída e validada em 2026-09-24.** Escopo: empacotar o OTel Java Agent na imagem Docker
e — como consequência natural, já que a Fase 2 tinha deixado isso como pendência bloqueada —
habilitar `JAVA_OPTS=-javaagent:...` no `docker-compose.yml` para validar telemetria real de
ponta a ponta chegando no SigNoz.

### O que foi feito
1. Em [Dockerfile](../../Dockerfile), no stage final (antes de `EXPOSE`/`USER spring`):
   adicionado `ADD --chown=spring:spring --checksum=sha256:... https://.../opentelemetry-javaagent.jar
   /app/opentelemetry-javaagent.jar`, baixando a **versão fixada v2.31.1** (release oficial do
   `open-telemetry/opentelemetry-java-instrumentation`) com verificação de checksum SHA-256
   embutida no próprio `ADD` (recurso do BuildKit, já habilitado via `# syntax=docker/dockerfile:1.7`
   no topo do arquivo) — builds não reprodutíveis ou arquivo corrompido/adulterado fazem o
   `docker build` falhar imediatamente.
2. `--chown=spring:spring` garante que o jar pertença ao usuário não-root `spring` (que já
   executa o processo via `USER spring`), sem precisar de `RUN chmod`/`chown` extra.
3. `ENV JAVA_OPTS=""` **mantido inalterado** no Dockerfile — o agent fica presente na imagem
   mas inerte por padrão; qualquer execução local (`mvn spring-boot:run`, que nem usa a imagem
   Docker) ou `docker run` direto sem sobrescrever `JAVA_OPTS` continua sem overhead do agent.
4. Em [docker-compose.yml](../../docker-compose.yml), serviço `app`: adicionada
   `JAVA_OPTS: -javaagent:/app/opentelemetry-javaagent.jar` (pendência deixada em aberto no
   registro da Fase 2, agora desbloqueada pelo jar estar empacotado na imagem).

### Bugs/obstáculos encontrados e resolvidos
- Nenhum bug/obstáculo nesta fase — build, subida do container e ativação do agent funcionaram
  na primeira tentativa. O checksum SHA-256 informado (obtido da página de release oficial no
  GitHub) validou corretamente contra o arquivo baixado pelo BuildKit.

### Testes de validação executados (todos com resultado ✅)
1. `docker compose build app`: `ADD --checksum=...` concluído sem erro de checksum (validação
   automática de integridade do jar).
2. `docker exec brewer-app sha256sum /app/opentelemetry-javaagent.jar` → hash **idêntico** ao
   pinado no Dockerfile; `ls -la` confirmou owner `spring:spring`.
3. `docker compose up -d app`: container sobe sem crash loop; logs mostram
   `io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.31.1`
   seguido do boot normal do Spring Boot e `"Started BrewerApplication in 18.591 seconds"`.
4. Tráfego gerado contra `http://localhost:8081/` (algumas requisições HTTP) e aguardado o
   agent exportar via OTLP gRPC para o `ingester`.
5. **Validação direta no ClickHouse do SigNoz** (`docker exec signoz-telemetrystore-clickhouse-0-0
   clickhouse-client --query ...`), consultando as tabelas de traces/métricas/logs — evidência
   mais forte do que checar a UI, pois prova que os dados persistiram no backend:
   - `signoz_traces.distributed_signoz_index_v3` (filtrando `serviceName = 'brewer-springboot'`):
     **40 spans** recebidos, entre outros: `GET /`, `OnCommittedResponseWrapper.sendRedirect`,
     múltiplas queries JDBC/JPA (`SELECT ... flyway_schema_history`, `information_schema`,
     `performance_schema` etc.) — confirma auto-instrumentação HTTP (Tomcat) **e** JDBC pelo
     agent, sem qualquer código customizado.
   - `signoz_metrics.distributed_time_series_v4` (filtrando `labels LIKE '%brewer-springboot%'`):
     **339 séries temporais** registradas — confirma pilar de métricas (JVM/runtime) também
     fluindo via o agent.
   - `signoz_logs.distributed_logs_v2` (filtrando `resources_string['service.name'] =
     'brewer-springboot'`): **33 registros de log** — confirma pilar de logs (via
     `OTEL_INSTRUMENTATION_LOGBACK_APPENDER_ENABLED=true`) também chegando correlacionado ao
     `service.name` correto.
6. Execução local sem Docker (`mvn spring-boot:run`) não foi reexecutada nesta sessão, mas
   permanece inerentemente não afetada: o agent só é referenciado via `JAVA_OPTS` do
   `docker-compose.yml`; o `docker-entrypoint.sh`/Dockerfile não são envolvidos na execução via
   Maven, e `ENV JAVA_OPTS=""` seguiria sendo o único default relevante caso alguém rodasse a
   imagem Docker sem overrides.

### Conclusão
Os três pilares de observabilidade (traces, métricas e logs) do `brewer-springboot` estão
fluindo de ponta a ponta para o SigNoz **apenas com auto-instrumentação do agent** — nenhum
código Java foi alterado nesta fase. As Fases 4 e 5 (actuator/micrometer-otlp e spans
customizados `@WithSpan` no fluxo de Vendas) continuam pendentes e são aditivas/opcionais sobre
essa base já funcional.

### Pendências para as próximas fases
- Fase 4: adicionar `spring-boot-starter-actuator`, `micrometer-registry-otlp` e
  `opentelemetry-instrumentation-annotations` ao `pom.xml` para health/metrics via Spring e
  suporte a `@WithSpan` manual.
- Fase 5: instrumentar manualmente o fluxo de Vendas (`CadastroVendaService`, `VendaListener`,
  `VendasController`, `Mailer`, `FotosController`) com `@WithSpan`.
- Fase 6 (opcional): documentar em `docs/11-observabilidade.md` e atualizar
  [08-execucao-local-e-docker.md](../08-execucao-local-e-docker.md) com a porta `8081` e os
  pré-requisitos do stack SigNoz/Foundry.
