# 10 · Modernização e Ferramentas Auxiliares

[⬅ Voltar ao README](../Readme.md)

Este projeto também serve como **catálogo de ferramentas de modernização/assessment** para
aplicações Java legadas, usadas ao longo da sua evolução (Spring Boot 2 → 4, atualização de Java,
containerização, avaliação de aptidão para nuvem). Os artefatos abaixo ficam versionados no próprio
repositório como referência.

## `.github/modernize/appcat` — Avaliação de aptidão para Azure (AppCAT)

- `assessment-config.yaml`: configuração de uma avaliação automatizada de código, com alvos de
  destino `azure-aks`, `azure-appservice`, `azure-container-apps` e modo `issue-only`;
- `assessment-plan.md`: registro da execução dessa avaliação, sumarizando os passos e onde consultar
  o relatório gerado;
- Utilidade prática: identificar antes de migrar para a nuvem quais trechos de código usam APIs
  específicas de plataforma (ex.: dependências de sistema de arquivos local, configuração hard-coded)
  que precisariam ser adaptados.

## `.github/modernize/java-upgrade` — Histórico de upgrade de versão de Java

- Diretório com metadados/relatórios do processo de upgrade de versão do JDK usado no projeto,
  útil como referência de "antes/depois" ao planejar upgrades semelhantes em outros projetos.

## `.azure/containerization-plan.copilotmd` — Plano de containerização

Documento que registra o passo a passo seguido para containerizar a aplicação:

1. Checagem de pré-requisitos (Docker instalado);
2. Análise do repositório (linguagem, framework, build system, porta exposta, dependências
   externas: MySQL, SMTP, S3);
3. Geração do `Dockerfile` (multi-stage, Maven → JRE Alpine);
4. Build e scan de vulnerabilidades da imagem gerada;
5. Criação de um `docker-compose.yml` para desenvolvimento local com MySQL (incluindo
   `healthcheck` e dependência de saúde entre serviços).

Esse plano é um bom exemplo de **checklist reutilizável** para containerizar qualquer aplicação
Spring Boot que dependa de um banco relacional externo.

## Relação com o restante da documentação

| Onde encontrar mais detalhes | Documento |
|---|---|
| Como construir/rodar a imagem Docker resultante | [08 · Execução Local e Docker](08-execucao-local-e-docker.md) |
| CI tradicional (Travis + SonarCloud) | [09 · Qualidade e Testes](09-qualidade-e-testes.md) |
| Dependências e versões usadas hoje | [04 · Catálogo Tecnológico](04-catalogo-tecnologico.md) |

> Este repositório **não contém** manifests de Kubernetes/OpenShift nem `Makefile` — essas
> ferramentas de orquestração fazem parte de um projeto irmão (versão Spring MVC do Brewer), fora do
> escopo desta base de código.

## Próxima leitura

- [01 · Visão Geral e Propósito](01-visao-geral-e-proposito.md)
