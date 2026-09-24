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
