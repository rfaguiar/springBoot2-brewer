# Plano de Migração para Spring Native (GraalVM Native Image)

> ## Status de execução (branch `feature/spring-native-migration`)
>
> Este plano foi parcialmente **executado** nesta branch, com um build real de imagem nativa
> via `mvn -Pnative spring-boot:build-image` (Cloud Native Buildpacks, JDK 25 + Maven local +
> Docker, sem necessidade de instalar GraalVM localmente). Resultado resumido:
>
> - ✅ **Fase 1 confirmada sem alterações no `pom.xml`**: os profiles `native`/`nativeTest`
>   (com `org.graalvm.buildtools:native-maven-plugin`) já vêm herdados de
>   `spring-boot-starter-parent:4.1.1` (verificado com `mvn help:all-profiles`), incluindo a
>   exclusão automática do `spring-boot-devtools` do build nativo.
> - ✅ **JasperReports removido do `pom.xml`** (`net.sf.jasperreports:jasperreports` e
>   `jasperreports-fonts`): confirmado por busca no código-fonte que a dependência **não é
>   usada em nenhum lugar** (nenhum `.jrxml`, nenhuma referência a `net.sf.jasperreports.*`).
>   Ela carregava um runtime Groovy embutido cujos inicializadores de classe (`CacheableCallSite`)
>   iniciam threads de background e retêm objetos ASM (`groovyjarjarasm.asm.Type`) que o
>   GraalVM native-image rejeita no heap da imagem. Suíte de testes (275 testes) validada
>   com `mvn test` após a remoção — **100% verde**, nenhuma regressão.
> - 🔴 **Bloqueador real encontrado (não previsto originalmente)**: `nz.net.ultraq.thymeleaf:
>   thymeleaf-layout-dialect` (usado de fato pelos templates via `layout:decorate`/
>   `layout:fragment` em `LayoutPadrao.html`/`LayoutSimples.html` e todas as telas) depende
>   obrigatoriamente (não opcional) de **Apache Groovy** — a própria biblioteca é implementada
>   em Groovy. O runtime de metaprogramação do Groovy (`groovy.lang.GroovySystem`,
>   `org.codehaus.groovy.reflection.ClassInfo`) mantém estado vivo (filas, locks, referências
>   fracas ligadas a um `ForkJoinPool`) que o GraalVM não permite congelar no heap da imagem
>   em build-time. Foram testadas 3 variações de `--initialize-at-run-time` (escopo crescente:
>   `org.codehaus.groovy` → `+groovyjarjarasm` → `+groovy`) e cada uma resolveu um erro apenas
>   para revelar outro erro equivalente em uma classe Groovy diferente — um padrão que indica
>   incompatibilidade estrutural, não uma hint pontual faltando.
> - ⏭️ **Próximo passo recomendado** (fora do escopo desta execução): substituir
>   `thymeleaf-layout-dialect` por fragmentos nativos do Thymeleaf (`th:insert`/`th:replace`/
>   `th:fragment`), eliminando a dependência de Groovy, e então repetir o build nativo. Essa
>   troca exige revisão de todos os templates HTML que usam `layout:decorate`/`layout:fragment`
>   (aproximadamente 12 arquivos em `src/main/resources/templates`), portanto foi tratada como
>   um item de acompanhamento e não foi executada nesta sessão.
> - ℹ️ `aws-java-sdk-s3` (AWS SDK v1) não chegou a ser exercitado no build porque o bean que o
>   usa (`S3Config`) está anotado com `@Profile("prod")`, então o processamento Spring AOT
>   (que roda sem esse profile ativo por padrão) não gera definição de bean para ele — o
>   risco documentado no item 3 permanece válido para quando o profile `prod` for ativado
>   durante o build/execução nativa.
>
> Nenhuma alteração de comportamento em produção foi feita: a única mudança de dependências
> (remoção do JasperReports) foi verificada como segura por análise estática de uso e pela
> suíte de testes completa.

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
| `spring-boot-starter-thymeleaf` + `thymeleaf-layout-dialect` + `thymeleaf-extras-data-attribute` + `thymeleaf-extras-springsecurity6` | ✅ Suportado, com ressalvas | Usar `spring.thymeleaf.cache=true` em produção nativa; validar extensões de terceiros com testes AOT |
| `spring-boot-starter-data-jpa` (Hibernate) | ✅ Suportado, com ressalvas | Hibernate possui suporte a native image desde Boot 3.x, mas *lazy-loading*/bytecode enhancement e proxies precisam ser validados nos testes |
| `spring-boot-starter-mail` | ✅ Suportado | — |
| `spring-boot-starter-security` | ✅ Suportado | Beans de `DaoAuthenticationProvider`/`SecurityConfig` já usam configuração declarativa, compatível com AOT |
| `spring-boot-starter-flyway` + `flyway-mysql` | ✅ Suportado | Migrations são lidas como resources; garantir que fiquem no classpath do artefato nativo |
| `mysql-connector-j` | ✅ Suportado (driver JDBC puro Java) | — |
| `spring-boot-devtools` | ⚠️ Deve ser excluído do build nativo | Spring Boot já desabilita/exclui devtools automaticamente ao empacotar; não requer ação manual |
| **`aws-java-sdk-s3` (AWS SDK v1)** | 🔴 Risco alto | SDK v1 não é otimizado para GraalVM (uso extensivo de reflection dinâmica, XML binding). Recomenda-se avaliar migração para **AWS SDK v2** (`software.amazon.awssdk:s3`), que possui melhor suporte a native image (incluindo módulo `url-connection-client` e hints publicados pela AWS) |
| **`net.sf.jasperreports` / `jasperreports-fonts`** | 🔴 Risco alto | JasperReports usa reflection, classloading dinâmico de expressões (Groovy/JavaScript) e recursos `.jasper` — normalmente exige `RuntimeHints` extensos e pode ter funcionalidades incompatíveis (ex. compilação dinâmica de relatórios). Precisa de spike técnico dedicado |
| `org.ehcache` | ⚠️ Risco médio | Requer hints de reflection/serialização para as classes de cache e `cache-api` (JSR-107) |
| `net.coobird:thumbnailator` | ⚠️ Risco médio | Usa `ImageIO`/reflection para plugins de formato de imagem; validar com testes |
| `commons-beanutils`, `commons-lang3` | ✅ Baixo risco | Bibliotecas amplamente testadas com native image, mas `commons-beanutils` usa reflection para introspecção — validar hints se usado diretamente no código |
| JUnit 4 / Mockito / AssertJ / EqualsVerifier (testes) | ✅ Suportado para testes AOT | Não afeta o artefato de produção; usado apenas em `test` scope |

## 4. Mudanças necessárias no `pom.xml`

1. Adicionar o profile `native` (herdado de `spring-boot-starter-parent`, já presente por padrão
   desde que o parent seja `spring-boot-starter-parent`), que ativa o plugin:
   ```xml
   <profiles>
     <profile>
       <id>native</id>
       <build>
         <plugins>
           <plugin>
             <groupId>org.graalvm.buildtools</groupId>
             <artifactId>native-maven-plugin</artifactId>
           </plugin>
         </plugins>
       </build>
     </profile>
   </profiles>
   ```
2. Confirmar que `spring-boot-maven-plugin` está presente (já está, ver `pom.xml` atual) — ele
   integra automaticamente com o plugin de native-image para gerar `spring-aot` sources.
3. Avaliar upgrade `aws-java-sdk-s3` → `software.amazon.awssdk:s3` (AWS SDK v2) como pré-requisito
   antes da fase de build nativo (ver seção 8, Fase 3).
4. Avaliar isolamento/substituição de JasperReports caso o spike técnico (Fase 3) mostre
   inviabilidade total em native image.

## 5. Runtime Hints necessários

- **Configuration properties aninhadas**: qualquer classe usada com `@ConfigurationProperties`
  que tenha propriedades aninhadas que **não sejam inner classes** deve usar
  `@NestedConfigurationProperty`, caso contrário o binding falhará em runtime nativo (hints de
  reflection não são gerados automaticamente para elas).
- **`RuntimeHintsRegistrar` customizado** provavelmente necessário para:
  - Templates/recursos do JasperReports (`.jrxml`/`.jasper` em `src/main/resources`).
  - Classes do AWS SDK (v1 ou v2) que usam reflection para (de)serialização de requests/responses.
  - Classes de modelo do EhCache/JCache usadas em serialização de cache.
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
  não declarados) — JasperReports e thumbnailator são candidatos a problemas aqui.
- Proxies JDK dinâmicos e CGLIB precisam ser conhecidos em build-time; Spring AOT já gera os
  hints para os proxies criados pelo próprio framework (ex. `@Transactional`, Spring Security),
  mas proxies criados manualmente no código da aplicação precisam de hints explícitos.
- Hibernate: lazy-loading e bytecode enhancement podem exigir ajustes (`hibernate.bytecode.provider=none`
  ou revisão de entidades com proxies lazy) — validar com testes de integração reais contra MySQL.
- Cache de templates do Thymeleaf deve ficar habilitado (`cache=true`) em produção nativa, já que
  não há reload em runtime de qualquer forma em uma imagem nativa imutável.
- Tempo de build nativo é significativamente maior que o build JVM tradicional — ajustar
  pipelines de CI (paralelização, cache de camada Docker, etc.).

## 8. Plano incremental de execução (fases)

1. **Fase 1 — Prova de conceito mínima**: habilitar o profile `native` no `pom.xml` e validar
   que um endpoint simples (ex. health-check) builda e executa como native image, sem as
   dependências de risco (JasperReports/AWS SDK) carregadas no caminho crítico.
2. **Fase 2 — Build completo da aplicação**: rodar `mvn -Pnative spring-boot:build-image` com
   todas as dependências atuais e catalogar todas as falhas de reflection/resource reportadas
   pelo `native-image` (via *tracing agent* `-agentlib:native-image-agent` se necessário para
   gerar configs de reflection/resources automaticamente).
3. **Fase 3 — Spike técnico e mitigação de dependências de risco**:
   - Testar JasperReports isoladamente em native image; se inviável, considerar (a) manter a
     geração de relatórios em um serviço/JVM separado (não nativo) ou (b) buscar alternativa
     (ex. geração de PDF via outra biblioteca com melhor suporte a native image).
   - Migrar `aws-java-sdk-s3` (v1) para `software.amazon.awssdk:s3` (v2) e validar upload/download
     de arquivos em modo nativo.
   - Adicionar hints de reflection/resource para EhCache e thumbnailator.
4. **Fase 4 — Validação funcional completa**: testar manualmente (ou via testes de integração
   automatizados) os fluxos críticos: login/autenticação (Spring Security), CRUD de cervejas/
   clientes/estilos, upload de imagem para S3, geração de relatório (venda/estoque), envio de
   e-mail, migrações Flyway na subida da aplicação nativa.
5. **Fase 5 — Ajuste de pipeline CI/CD e empacotamento**: atualizar `Dockerfile`/`.travis.yml`
   (ou pipeline equivalente) para construir e publicar a imagem nativa via buildpacks, mantendo
   uma imagem JVM tradicional como *fallback* até que a Fase 3 esteja totalmente resolvida.

## 9. Riscos e recomendações

- **Não migrar tudo de uma vez**: dado o risco alto identificado em JasperReports e AWS SDK v1,
  recomenda-se tratar a migração para native image como incremental, mantendo a build JVM atual
  em produção até que as Fases 3 e 4 estejam validadas.
- **Considerar arquitetura híbrida**: se JasperReports permanecer incompatível, extrair a geração
  de relatórios para um módulo/serviço separado executado em JVM tradicional, mantendo o restante
  da aplicação (web, segurança, persistência) como native image.
- **Investir em testes automatizados antes da migração**: como o modo nativo remove flexibilidade
  de reflection em runtime, uma suíte de testes de integração robusta (já parcialmente existente,
  ver `pitest-maven` configurado no `pom.xml`) é essencial para detectar regressões de
  comportamento causadas pelo processamento AOT.
- **Medir ganhos antes de comprometer o roadmap**: validar com benchmarks reais (tempo de
  startup, uso de memória) se o ganho justifica o esforço de manutenção adicional (hints,
  builds mais lentos, superfície de testes maior).
