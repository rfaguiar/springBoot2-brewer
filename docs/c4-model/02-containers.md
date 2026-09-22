# 02 · Diagrama de Contêineres (C4 Nível 2)

[⬅ Voltar ao Modelo C4](README.md)

Faz o zoom dentro do sistema Brewer e mostra as "unidades de deploy" (contêineres, no sentido do C4
model — não necessariamente containers Docker, embora aqui coincidam) e como elas se comunicam.

```mermaid
flowchart TB
    colaborador["👤 Colaborador da Distribuidora<br/><i>[Pessoa]</i>"]

    subgraph brewerBoundary["Brewer [Fronteira do Sistema]"]
        webapp["🖥️ Aplicação Web<br/><i>[Contêiner: Spring Boot 4 / Java 25]</i><br/>Monólito Spring MVC + Thymeleaf (SSR).<br/>Controllers, services, segurança e views<br/>renderizadas no servidor. Tomcat embarcado, porta 8080"]

        db[("🗄️ Banco de Dados<br/><i>[Contêiner: MySQL 8]</i><br/>Armazena cervejas, clientes, vendas,<br/>usuários, grupos/permissões etc.<br/>Schema versionado via Flyway")]

        localdisk["💾 Armazenamento Local de Fotos<br/><i>[Contêiner: Sistema de Arquivos]</i><br/>Disco do container/host onde as fotos<br/>são salvas por padrão (perfil local/dev)"]
    end

    sendgrid["📧 SendGrid<br/><i>[Sistema Externo]</i>"]
    s3["☁️ Amazon S3<br/><i>[Sistema Externo]</i>"]

    colaborador -->|"HTTPS :8080<br/>(formulários HTML)"| webapp
    webapp -->|"JDBC<br/>(Spring Data JPA / Hibernate)"| db
    webapp -->|"Leitura/escrita de arquivos<br/>(perfil local/dev, FotoStorageLocal)"| localdisk
    webapp -->|"SMTP :587<br/>(Mailer / JavaMailSender)"| sendgrid
    webapp -->|"AWS SDK S3<br/>(perfil prod, FotoStorageS3)"| s3

    classDef person fill:#08427b,color:#fff,stroke:#052e56;
    classDef container fill:#1168bd,color:#fff,stroke:#0b4884;
    classDef db fill:#438dd5,color:#fff,stroke:#2e6295;
    classDef external fill:#999999,color:#fff,stroke:#6b6b6b;

    class colaborador person
    class webapp container
    class db db
    class localdisk container
    class sendgrid,s3 external
```

## Mapeamento para Docker / Docker Compose

| Contêiner (C4) | Serviço em `docker-compose.yml` | Observação |
|---|---|---|
| Aplicação Web | `app` | Build multi-stage via `Dockerfile` (Maven → JRE Alpine), roda como usuário não-root |
| Banco de Dados | `mysql` | MySQL 8, com `healthcheck`; `docker-entrypoint.sh` aguarda a porta responder antes de subir a app |
| Armazenamento Local | *(volume/diretório dentro do container `app`)* | Caminho configurável via `BREWER_FOTO_STORAGE_LOCAL_PATH` |

Detalhes de execução local/Docker: [08 · Execução Local e Docker](../08-execucao-local-e-docker.md).

## Notas de deploy

- Apenas **um contêiner de aplicação** (monólito) — não há separação em microsserviços, gateway de
  API ou frontend SPA separado.
- A escolha entre `localdisk` e `s3` para fotos é feita em tempo de execução pelo padrão **Strategy**
  (`FotoStorage` → `FotoStorageLocal` ou `FotoStorageS3`), conforme o perfil Spring ativo.
- `spring-boot-devtools` (escopo runtime) habilita restart automático apenas em desenvolvimento, sem
  impacto nos contêineres de produção.

## Próxima leitura

- [03 · Diagrama de Componentes](03-componentes.md)
- [01 · Diagrama de Contexto](01-contexto.md)
