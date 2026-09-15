## Docker

Build image:

```bash
docker build -t springboot2-brewer:v1 -t springboot2-brewer:latest --platform linux/amd64 .
```

Run container:

```bash
docker run --rm -p 8080:8080 -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/brewer-springboot?useSSL=false" -e SPRING_DATASOURCE_USERNAME="root" -e SPRING_DATASOURCE_PASSWORD="root" -e BREWER_EMAIL_USERNAME="" -e BREWER_EMAIL_PASSWORD="" springboot2-brewer:latest
```

Optional env vars:
- `BREWER_FOTO_STORAGE_LOCAL_URL_BASE`
- `BREWER_FOTO_STORAGE_LOCAL_PATH`
- `JAVA_OPTS`

### Docker Compose (app + MySQL)

Para subir a aplicação junto com o banco MySQL localmente:

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

