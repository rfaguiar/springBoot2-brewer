# 11 · Observabilidade (OpenTelemetry + SigNoz)

[⬅ Voltar ao README](../Readme.md)

Este projeto tem instrumentação completa de observabilidade (traces, métricas e logs)
via **OpenTelemetry**, com backend **SigNoz** (all-in-one, self-hosted via Docker). O registro
completo de decisões e implementação está em
[docs/decisions/plan-openTelemetrySignozObservability.md](decisions/plan-openTelemetrySignozObservability.md).

## Arquitetura

- **OpenTelemetry Java Agent** (`opentelemetry-javaagent.jar`, versão fixada, baixado no
  `Dockerfile` com checksum SHA-256) — auto-instrumentação zero-code de HTTP (Tomcat), JDBC/JPA,
  logs (correlação via MDC) e métricas de runtime (JVM), ativado via `-javaagent` apenas quando
  o container roda pelo `docker-compose.yml` (execução local com `mvn spring-boot:run` continua
  sem overhead algum do agent).
- **Spring Boot Actuator + Micrometer OTLP** (`micrometer-registry-otlp`) — segunda fonte de
  métricas (JVM, HikariCP, HTTP), independente do agent, exportada via
  `management.otlp.metrics.export.url`. Endpoints `/actuator/health` (com grupos
  liveness/readiness) e `/actuator/metrics` expostos, mas **protegidos pelo mesmo
  `SecurityConfig`** da aplicação (exigem usuário autenticado, sem exceção anônima).
- **Spans customizados manuais** (`@WithSpan`, `opentelemetry-instrumentation-annotations`) no
  fluxo de negócio de Vendas e Fotos, complementando a auto-instrumentação HTTP (que sozinha não
  distingue ações que compartilham a mesma rota, ex.: `POST /vendas/nova` usado por
  salvar/emitir/enviarEmail/cancelar):
  - `venda.emitir`, `venda.cancelar` (`CadastroVendaService`)
  - `venda.baixar-estoque` (`VendaListener`, listener síncrono do evento de emissão)
  - `vendas.emitir`, `vendas.cancelar`, `vendas.enviar-email` (`VendasController`)
  - `mailer.enviar-confirmacao-venda` (`Mailer`, método `@Async`)
  - `fotos.upload`, `fotos.recuperar` (`FotosController`)
- **SigNoz** (backend all-in-one: ClickHouse para armazenamento, coletor OTLP `ingester`,
  UI/API `signoz-signoz-0`) — provisionado via **Foundry** (`foundryctl`), não mais via
  docker-compose estático (método legado descontinuado pelo projeto SigNoz). Config em
  [observability/casting.yaml](../observability/casting.yaml); stack gerado em
  `observability/pours/` (ignorado no git, gerado localmente).

## Portas e rede

| Serviço | Porta(s) host | Observação |
|---|---|---|
| App (`brewer-app`) | **8081** → 8080 (container) | Porta alterada de 8080 para 8081 pois a UI do SigNoz já usa 8080 |
| SigNoz UI/API (`signoz-signoz-0`) | 8080 | `http://localhost:8080` |
| Coletor OTLP (`ingester`) | 4317 (gRPC) / 4318 (HTTP) | Destino de `OTEL_EXPORTER_OTLP_ENDPOINT` / `management.otlp.metrics.export.url` |
| MySQL (`brewer-mysql`) | 3306 | Inalterado |

O serviço `app` do [docker-compose.yml](../docker-compose.yml) está conectado a **duas redes
Docker**: a rede `default` (para falar com o `mysql`) e a rede externa `brewer-observability`
(para falar com o `ingester` do SigNoz pelo hostname `ingester`). Essa rede externa é criada uma
vez com `docker network create brewer-observability` e compartilhada entre os dois stacks
(aplicação e SigNoz).

## Variáveis de ambiente (já configuradas no `docker-compose.yml`)

| Variável | Valor | Para quê |
|---|---|---|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://ingester:4317` | Destino OTLP gRPC do agent (traces/logs) |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `grpc` | Protocolo do agent |
| `OTEL_SERVICE_NAME` | `brewer-springboot` | Nome do serviço nos traces/métricas/logs |
| `OTEL_RESOURCE_ATTRIBUTES` | `service.namespace=brewer,deployment.environment=docker` | Atributos de resource |
| `OTEL_TRACES_EXPORTER` / `OTEL_METRICS_EXPORTER` / `OTEL_LOGS_EXPORTER` | `otlp` | Habilita os 3 pilares no agent |
| `OTEL_INSTRUMENTATION_LOGBACK_APPENDER_ENABLED` | `true` | Correlação de logs (trace_id/span_id) |
| `MANAGEMENT_OTLP_METRICS_EXPORT_URL` | `http://ingester:4318/v1/metrics` | Endpoint OTLP HTTP do Micrometer (actuator) |
| `JAVA_OPTS` | `-javaagent:/app/opentelemetry-javaagent.jar` | Ativa o agent (só no container; `ENV JAVA_OPTS=""` é o default do Dockerfile) |

## Subindo o ambiente completo localmente

1. **Pré-requisito único, feito uma vez**: instalar o `foundryctl` (ver registro de
   implementação da Fase 1 no plano de decisões) e criar a rede externa:
   ```powershell
   docker network create brewer-observability
   ```
2. **Subir o SigNoz** (a partir da pasta `observability/`):
   ```powershell
   cd observability
   ..\.tools\foundry_windows_amd64\bin\foundryctl.exe cast -f casting.yaml *> cast-output.log
   ```
   Ou, se o stack já tiver sido gerado antes (`observability/pours/deployment/compose.yaml`
   existente):
   ```powershell
   docker compose -f observability/pours/deployment/compose.yaml up -d
   ```
   **Na primeira vez**, acesse `http://localhost:8080/signup` e crie a conta/organização inicial
   — o coletor OTLP (`ingester`) só ativa os receivers 4317/4318 depois que existe uma
   organização no SigNoz (ver detalhes no registro da Fase 1 do plano de decisões).
3. **Subir a aplicação** (a partir da raiz do repositório):
   ```powershell
   docker compose up --build -d
   ```
4. Acessar a aplicação em `http://localhost:8081/login` e o SigNoz em `http://localhost:8080`.
   Após alguns minutos de uso, o serviço `brewer-springboot` aparece na lista de serviços do
   SigNoz, com traces, métricas e logs correlacionados.

> Execução local sem Docker (`mvn spring-boot:run`) continua funcionando normalmente, sem
> qualquer dependência do SigNoz — o agent só é ativado via `JAVA_OPTS` do `docker-compose.yml`.

## Riscos conhecidos

- **ClickHouse Keeper + Docker Desktop no Windows**: risco documentado oficialmente pelo SigNoz
  de crash loop (segfault) sob a virtualização do Docker Desktop no Windows. **Não se
  materializou** nos testes realizados (containers estáveis por 3h+ de uso contínuo), mas
  permanece como risco a monitorar. Mitigação recomendada pelo SigNoz, caso ocorra: rodar Docker
  Engine nativo dentro do WSL2 em vez do Docker Desktop.

## Referências

- [Plano de decisões e registros de implementação por fase](decisions/plan-openTelemetrySignozObservability.md)
  — histórico completo de decisões, bugs encontrados/corrigidos e testes de validação de cada
  fase (provisionamento do SigNoz, rede, agent, actuator/métricas, spans customizados).
