# 🍺 brewer-springboot

Sistema web de gestão de vendas para uma distribuidora de cervejas artesanais — cadastro de
produtos, clientes, usuários (RBAC), emissão de vendas com baixa de estoque, dashboard e envio de
e-mail com PDF de confirmação. Construído com **Spring Boot 4** e **Java 25**.

Este projeto nasceu como estudo de caso da Algaworks e evoluiu para um **laboratório de
modernização Java** (upgrades de framework/JDK, containerização, avaliação de aptidão para nuvem,
qualidade de testes via mutation testing). A documentação em [`docs/`](docs/) foi escrita para
servir como **catálogo de consulta rápida de tecnologias e padrões** do dia a dia de um(a)
engenheiro(a) de software — cada tópico é independente e pode ser lido isoladamente.

## 📚 Documentação

| # | Documento | O que você encontra lá |
|---|---|---|
| 01 | [Visão Geral e Propósito](docs/01-visao-geral-e-proposito.md) | O que é o sistema, domínio de negócio e motivação do projeto |
| 02 | [Arquitetura](docs/02-arquitetura.md) | Camadas MVC, padrões de projeto aplicados, fluxo de uma requisição e [Modelo C4 completo](docs/c4-model/README.md) (Contexto, Contêineres, Componentes, Dinâmico) |
| 03 | [Estrutura de Pastas](docs/03-estrutura-de-pastas.md) | Mapa completo dos pacotes Java e recursos do projeto |
| 04 | [Catálogo Tecnológico](docs/04-catalogo-tecnologico.md) | Toda dependência do `pom.xml`, versão, categoria e para que serve |
| 05 | [Features](docs/05-features.md) | Funcionalidades de negócio por módulo (cervejas, clientes, vendas, RBAC, dashboard...) |
| 06 | [Segurança](docs/06-seguranca.md) | Spring Security, autenticação, RBAC dinâmico, sessão |
| 07 | [Persistência e Dados](docs/07-persistencia-e-dados.md) | JPA, Flyway, consultas dinâmicas, paginação, cache |
| 08 | [Execução Local e Docker](docs/08-execucao-local-e-docker.md) | Como rodar com Maven, Docker e Docker Compose |
| 09 | [Qualidade e Testes](docs/09-qualidade-e-testes.md) | Stack de testes, mutation testing (PIT) e CI |
| 10 | [Modernização e Ferramentas Auxiliares](docs/10-modernizacao-e-ci.md) | Assessment de nuvem (AppCAT), plano de containerização |
| 11 | [Observabilidade (OpenTelemetry + SigNoz)](docs/11-observabilidade.md) | Traces, métricas e logs via OTel Java Agent + SigNoz, spans customizados no fluxo de Vendas |

## 🚀 Quick start

```bash
# build + testes
mvn clean install

# subir app + MySQL localmente com um comando
docker compose up --build -d
```

Depois acesse: `http://localhost:8080/login`

Para detalhes completos (variáveis de ambiente, build de imagem, comandos de log/limpeza), veja
[08 · Execução Local e Docker](docs/08-execucao-local-e-docker.md).

## 🛠️ Stack principal

Spring Boot 4 · Java 25 · Spring MVC · Spring Security 6 · Spring Data JPA · Flyway · MySQL ·
Thymeleaf · JasperReports · AWS SDK (S3) · Docker/Docker Compose. Lista completa e detalhada em
[04 · Catálogo Tecnológico](docs/04-catalogo-tecnologico.md).

