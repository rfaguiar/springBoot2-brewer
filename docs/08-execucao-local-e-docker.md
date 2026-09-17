# 08 · Execução Local e Docker

[⬅ Voltar ao README](../Readme.md)

## Pré-requisitos

- JDK 25
- Maven (ou o `mvnw`, se disponível)
- MySQL 8 (local ou via container — ver seção Docker Compose abaixo)

## Rodando localmente com Maven

```bash
mvn clean install
mvn spring-boot:run
```

A aplicação sobe por padrão na porta `8080` e usa as configurações de `application.properties`
(perfil padrão), que já traz *defaults* locais via variáveis de ambiente com fallback (ex.:
`spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/brewer-springboot?useSSL=false}`).

Variáveis de ambiente relevantes (todas opcionais em ambiente local, pois têm valor padrão):

| Variável | Uso |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do MySQL |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Credenciais do banco |
| `BREWER_EMAIL_USERNAME` / `BREWER_EMAIL_PASSWORD` | Credenciais de envio de e-mail |
| `BREWER_FOTO_STORAGE_LOCAL_URL_BASE` | URL base para servir fotos salvas localmente |
| `BREWER_FOTO_STORAGE_LOCAL_PATH` | Diretório local onde as fotos são salvas |
| `JAVA_OPTS` | Opções extras da JVM |

## Docker

Build da imagem:

```bash
docker build -t springboot2-brewer:v1 -t springboot2-brewer:latest --platform linux/amd64 .
```

Rodar o container (apontando para um MySQL já em execução, ex.: no host):

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/brewer-springboot?useSSL=false" \
  -e SPRING_DATASOURCE_USERNAME="root" \
  -e SPRING_DATASOURCE_PASSWORD="root" \
  -e BREWER_EMAIL_USERNAME="" \
  -e BREWER_EMAIL_PASSWORD="" \
  springboot2-brewer:latest
```

Variáveis opcionais adicionais suportadas pelo container:
- `BREWER_FOTO_STORAGE_LOCAL_URL_BASE`
- `BREWER_FOTO_STORAGE_LOCAL_PATH`
- `JAVA_OPTS`

O `Dockerfile` faz build multi-stage (Maven → JRE Alpine), roda como usuário não-root e aplica
patch de pacotes críticos (`openssl`, `expat`) na imagem base, por segurança.

## Docker Compose (app + MySQL)

Para subir a aplicação junto com o banco MySQL localmente, em um único comando:

```bash
docker compose up --build -d
```

Aguarde o MySQL ficar saudável e a aplicação aplicar as migrações do Flyway, depois acesse:

```
http://localhost:8080/login
```

Ver logs:

```bash
docker compose logs -f app
docker compose logs -f mysql
```

Derrubar o stack (mantendo os dados do banco):

```bash
docker compose down
```

Derrubar e apagar os dados do banco:

```bash
docker compose down -v
```

> O `docker-entrypoint.sh` do container aguarda ativamente a porta do MySQL responder antes de
> iniciar a aplicação — uma proteção extra além do `healthcheck` do Compose, para o caso do MySQL
> reiniciar internamente durante o primeiro bootstrap.

## Próxima leitura

- [04 · Catálogo Tecnológico](04-catalogo-tecnologico.md)
- [09 · Qualidade e Testes](09-qualidade-e-testes.md)
- [10 · Modernização e CI](10-modernizacao-e-ci.md)
