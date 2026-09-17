# 09 · Qualidade e Testes

[⬅ Voltar ao README](../Readme.md)

## Stack de testes

- **JUnit 4** como runner principal dos testes;
- **Mockito** para mocks/stubs de colaboradores;
- **AssertJ** e **Hamcrest** para asserções fluentes e expressivas;
- **H2 Database** como banco em memória para testes de integração/repositório, sem depender de um
  MySQL real;
- **Spring Test** (`spring-test`) para testes de contexto Spring/MVC;
- **EqualsVerifier** para validar de forma automatizada os contratos de `equals()`/`hashCode()` das
  entidades de domínio (`com.brewer.model.*`).

Rodar a suíte completa:

```bash
mvn test
```

## Mutation Testing (PIT / pitest)

Além da cobertura de linha tradicional, o projeto usa **[PIT (pitest)](https://pitest.org/)**
(`pitest-maven` `1.19.1`, configurado em `pom.xml`) para medir a **qualidade real dos testes**:
o PIT introduz pequenas mutações no bytecode (ex.: trocar `+` por `-`, inverter uma condição) e
verifica se a suíte de testes é capaz de "matar" (detectar) essas mutações. Cobertura de linha alta
não garante testes que realmente verificam comportamento — o *mutation score* expõe essa lacuna.

- Escopo analisado (`targetClasses`): `com.brewer.service.*`, `com.brewer.controller.*`,
  `com.brewer.model.*`, `com.brewer.repository.helper.*`, `com.brewer.storage.*`,
  `com.brewer.session.*`, `com.brewer.security.*`, `com.brewer.validation.*`;
- Testes considerados (`targetTests`): `com.brewer.*`.

Como reproduzir localmente (requer um JDK 21, pois as bibliotecas de instrumentação do PIT ainda
não suportam totalmente bytecode do Java 25):

```powershell
$env:JAVA_HOME = "<caminho para JDK 21>"
$env:PATH = "<caminho maven>\bin;$env:JAVA_HOME\bin;$env:PATH"
mvn -Dmaven.compiler.release=21 clean test-compile org.pitest:pitest-maven:mutationCoverage
```

O relatório HTML é gerado em `target/pit-reports/index.html`.

### Resultado da última execução documentada

📄 Relatório completo, análise por pacote e plano de ação: **[mutation-testing-plan.md](mutation-testing-plan.md)**.

| Métrica | Baseline | Após plano de ação |
|---|---:|---:|
| Mutantes gerados | 685 | 685 |
| **Mutation score** | ~61% | **83%** |
| Cobertura de linha | 92% | 97% |
| Test strength | 73% | 89% |

Principais aprendizados registrados no plano: grande parte dos mutantes sobreviventes estava
concentrada em métodos `equals`/`hashCode`/`toString` gerados automaticamente e em código sem
verificação de interação (`Mockito.verify`) — um padrão comum em bases de código com boa cobertura
de linha, porém asserts fracos.

## Integração Contínua (CI)

Pipeline definido em `.travis.yml` (Travis CI):

- `mvn clean install` — build e execução da suíte de testes a cada push;
- Análise estática de código com **SonarCloud**, executada via
  `mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar` (cobertura via JaCoCo
  alimentando o Sonar).

> Nota: o `.travis.yml` original também continha um passo de *deploy* automático para o Heroku.
> Esse passo é específico de um ambiente de hospedagem legado e foi **intencionalmente omitido**
> desta documentação — o foco atual de execução/deploy é local via Docker/Compose
> (ver [08 · Execução Local e Docker](08-execucao-local-e-docker.md)) e nuvem via Azure
> (ver [10 · Modernização e CI](10-modernizacao-e-ci.md)).

## Próxima leitura

- [07 · Persistência e Dados](07-persistencia-e-dados.md)
- [10 · Modernização e CI](10-modernizacao-e-ci.md)
