# Plano de Testes Baseado em Mutation Testing (PIT/pitest)

## 1. Ferramenta utilizada

- **Framework**: [PIT (pitest)](https://pitest.org/) `pitest-maven` `1.19.1`
- **Integração**: plugin adicionado em `pom.xml` (seção `<build><plugins>`), sem alterar o build padrão do projeto.
- **Escopo analisado (`targetClasses`)**: `com.brewer.service.*`, `com.brewer.controller.*`, `com.brewer.model.*`, `com.brewer.repository.helper.*`, `com.brewer.storage.*`, `com.brewer.session.*`, `com.brewer.security.*`, `com.brewer.validation.*`
- **Testes usados (`targetTests`)**: `com.brewer.*` (as 159 classes de teste existentes em `src/test/java`)

### Como reproduzir a execução

O projeto está configurado para Java 25, mas as bibliotecas de instrumentação do PIT (ASM) e os mocks dinâmicos (Mockito inline mock maker / Hibernate proxies) ainda não suportam bytecode de major version 69 (Java 25). Para rodar localmente, use um JDK 21 e force a compilação para o mesmo alvo:

```powershell
$env:JAVA_HOME = "<caminho para JDK 21>"
$env:PATH = "<caminho maven>\bin;$env:JAVA_HOME\bin;$env:PATH"
mvn -Dmaven.compiler.release=21 clean test-compile org.pitest:pitest-maven:mutationCoverage
```

O relatório HTML é gerado em `target/pit-reports/index.html` e o XML bruto em `target/pit-reports/mutations.xml` (usado para a análise abaixo).

> Observação: os parâmetros `timeoutConstant=15000`, `timeoutFactor=3` e `threads=4` foram adicionados à configuração do plugin porque o agente do Mockito (self-attach) atrasa a inicialização de cada JVM "minion" e causava falsos `TIMED_OUT` com os valores padrão.

## 2. Resultado geral da execução

| Métrica | Valor |
|---|---|
| Mutantes gerados | 685 |
| Mutantes mortos (KILLED) | 414 (60%) |
| Mutantes sobreviventes (SURVIVED) | 152 |
| Mutantes sem cobertura (NO_COVERAGE) | 118 |
| Timeout | 1 |
| **Mutation score** | **~61%** |
| Cobertura de linha (classes mutadas) | 92% (1182/1288) |
| Test strength (mutantes cobertos que morreram) | 73% |

**Leitura**: a cobertura de linha é alta (92%), mas o *mutation score* é bem mais baixo (61%). Isso indica que grande parte dos testes existentes **exercita** o código mas **não verifica comportamento** (faltam asserts fortes, testes de `equals`/`hashCode`, e testes de "efeito colateral" de métodos void).

## 3. Mutation score por pacote

| Pacote | Mutantes | Mortos | Score | Prioridade |
|---|---:|---:|---:|---|
| `com.brewer.security` | 15 | 2 | **13%** | 🔴 Crítico |
| `com.brewer.controller.page` | 23 | 6 | **26%** | 🔴 Crítico |
| `com.brewer.repository.helper.venda` | 29 | 10 | **35%** | 🔴 Crítico |
| `com.brewer.service.event.venda` | 3 | 1 | 33% | 🟠 Alto |
| `com.brewer.model` | 292 | 149 | 51% | 🟠 Alto (maior volume) |
| `com.brewer.storage.s3` | 20 | 10 | 50% | 🟠 Alto |
| `com.brewer.session` | 42 | 25 | 60% | 🟡 Médio |
| `com.brewer.repository.helper.usuario` | 18 | 12 | 67% | 🟡 Médio |
| `com.brewer.repository.helper.cliente` | 10 | 7 | 70% | 🟡 Médio |
| `com.brewer.repository.helper.cidade` | 10 | 7 | 70% | 🟡 Médio |
| `com.brewer.repository.helper.estilo` | 9 | 6 | 67% | 🟡 Médio |
| `com.brewer.repository.helper.cerveja` | 22 | 16 | 73% | 🟢 Baixo |
| `com.brewer.controller` | 104 | 85 | 82% | 🟢 Baixo |
| `com.brewer.storage.local` | 11 | 9 | 82% | 🟢 Baixo |
| `com.brewer.controller.validator` | 14 | 12 | 86% | 🟢 Baixo |
| `com.brewer.model.validation` | 5 | 4 | 80% | 🟢 Baixo |
| `com.brewer.validation.validator` | 6 | 5 | 83% | 🟢 Baixo |
| `com.brewer.controller.converter` | 12 | 12 | **100%** | ✅ OK |

## 4. Padrão sistêmico identificado: `equals`/`hashCode`/`toString`

A maior fonte de mutantes sobreviventes/sem cobertura está nos métodos `equals`, `hashCode` e `toString` gerados (IDE/Lombok-style) das entidades e VOs: `UsuarioGrupoId`, `Venda`, `Cliente`, `Endereco`, `Cidade`, `Estilo`, `BaseEntity`, `Permissao`, `Cerveja`, `Grupo`, `ItemVenda`, `Estado`, `UsuarioGrupo`, `Usuario`, `TipoPessoa`, `TabelaItensVenda`, `UsuarioSistema`. Nenhum desses testes cobre casos como:

- comparar com `null`;
- comparar com objeto de outra classe;
- comparar dois objetos com apenas **um** campo diferente (para cada campo);
- verificar `hashCode()` consistente entre objetos iguais e diferente para objetos com campos diferentes;
- `toString()` não vazio.

**Ação recomendada (transversal)**: criar/expandir testes parametrizados de `equals`/`hashCode`/`toString` para cada entidade, cobrindo os casos acima. Como é um padrão repetido, recomenda-se um helper de teste reutilizável (ex.: `EqualsHashCodeTestSupport`) ou uso da lib `nl.jqno.equalsverifier:equalsverifier` (adicionar como dependência de teste) para eliminar a maior parte destes mutantes com pouco código.

## 5. Plano de ação por classe (top prioridades)

### 🔴 5.1 `com.brewer.security.UsuarioSistema` e `AppUserDetailsService` (score 13%)
Classe de autenticação (`UserDetails`) sem nenhum teste dedicado a `equals`/`hashCode`/`getUsuario()`/`toString()`.
- [ ] Criar `UsuarioSistemaTest`: `equals` com null, outra classe, mesmo `Usuario`, `Usuario` diferente; `hashCode` consistente; `getUsuario()` retorna a instância injetada; `toString()` não vazio.
- [ ] Em `AppUserDetailsServiceTest`, adicionar teste que verifica que `getPermissoes` realmente popula a lista com as `GrantedAuthority` de **cada** grupo/permissão (mutante: `removed call to List::forEach` sobreviveu — teste atual provavelmente só verifica tamanho ou não verifica o conteúdo).

### 🔴 5.2 `com.brewer.controller.page.PageWrapper` (score 26%)
Classe de paginação usada nas views, sem teste unitário próprio (`PageWrapperTest` existe mas não verifica valores de retorno).
- [ ] `getAtual()` e `getTotal()`: asserts com valores reais (mutante trocou retorno por `0` e sobreviveu).
- [ ] `isPrimeira()`/`isUltima()`/`isVazia()`/`ordenada()`/`descendente()`: testar ambos os ramos (true/false) explicitamente.
- [ ] `inverterDirecao()`, `urlOrdenada()`, `urlParaPagina()`: assert de string exata (não vazia) — mutantes `EmptyObjectReturnValsMutator` sobreviveram.
- [ ] Construtor: cobrir o `if` de "sem HttpServletRequest / sem ordenação" (mutante `NegateConditionalsMutator` na linha 20).

### 🔴 5.3 `com.brewer.repository.helper.venda.VendasImpl` (score 35%)
- [ ] `filtrar(...)`: assert de que `PaginacaoUtil.preparar` é realmente chamado/aplicado (verificar efeito na paginação, não só o retorno).
- [ ] `total(...)`: assert do valor `Long` retornado (não apenas "não nulo") e verificar que `adicionarFiltro` é chamado (ex.: via `Mockito.verify` ou validando a query gerada).
- [ ] `totalPorMes` / `totalPorOrigem`: **sem nenhuma cobertura** (`NO_COVERAGE`) nos loops internos e lambdas — criar testes que exercitem múltiplos meses/origens, incluindo caso de lista vazia e caso com mais de um mês/origem no resultado (cobre `MathMutator`, `ConditionalsBoundaryMutator`, `NegateConditionalsMutator` nos laços de preenchimento de meses faltantes).

### 🟠 5.4 `com.brewer.model.Venda` (16 sobreviventes) e `VendasController` (16 sobreviventes)
- [ ] `Venda.calcularValorTotal()`: assert do valor calculado exato (subtotal + frete − desconto), incluindo casos com desconto/frete zero e não-zero.
- [ ] `Venda.adicionarItens(...)`: verificar que cada item recebeu `setVenda(this)` (mutante removeu o `forEach`/`setVenda` e sobreviveu).
- [ ] `Venda.getDiasCriacao()`, `getUsuario()`, `getValorDesconto()`, `getValorFrete()`: asserts de valor, não apenas presença.
- [ ] `VendasController`: os testes de `salvar`, `emitir`, `enviarEmail`, `adicionarItem`, `alterarQuantidadeItem`, `excluirItem`, `editar` e `validarVenda` usam mocks mas **não verificam interações** (`Mockito.verify(...)`) — adicionar `verify()` para: `validarVenda(...)`, `venda.setUsuario(...)`, `cadastroVendaService.emitir(...)`, `mailer.enviar(...)`, `tabelasItensSession.adicionarItem/alterarQuantidadeItens/excluirItem(...)`, `venda.adicionarItens(...)`, `venda.calcularValorTotal()`.

### 🟠 5.5 `com.brewer.storage.s3.FotoStorageS3` (score 50%)
- [ ] `enviarFoto`/`enviarThumbnail`: verificar via `Mockito.verify` que `ObjectMetadata.setContentType(...)` e `setContentLength(...)` são chamados com os valores corretos.
- [ ] `salvar(...)`: assert do nome de arquivo/URL retornado (não vazio) e verificar chamada a `AccessControlList.grantPermission(...)` (ACL pública de leitura).
- [ ] `excluir(...)`: cobrir explicitamente o retorno `true` (sucesso) e `false` (falha), hoje um dos dois ramos sobrevive/não é coberto.

### 🟠 5.6 `com.brewer.service.event.venda.VendaListener` (score 33%)
- [ ] `vendaEmitida(...)`: assert do valor exato de `quantidadeEstoque` após o evento (mutante trocou subtração por soma e sobreviveu) e `verify` de `Cerveja.setQuantidadeEstoque(...)`.

### 🟡 5.7 `com.brewer.session.TabelaItensVenda` / `TabelasItensSession`
- [ ] Completar `equals`/`hashCode` (ver seção 4) — nenhum ramo de `equals` está coberto.

### 🟢 5.8 Repositórios `helper.*Impl` restantes (cerveja, cidade, cliente, estilo, usuario)
- [ ] Revisar métodos de filtro/paginação: garantir asserts sobre o conteúdo da lista retornada (não só `size()`), e testar filtros combinados/vazios para eliminar mutantes de `NegateConditionalsMutator`/`ConditionalsBoundaryMutator` remanescentes.

### 🟢 5.9 `com.brewer.controller.validator.VendaValidator`
- [ ] Testar o limite exato de `validarValorTotalNegativo` (valor `0` deve ser válido, `-0.01` inválido — cobre `ConditionalsBoundaryMutator`).
- [ ] Verificar que `errors.rejectIfEmpty(...)` é chamado quando a lista de itens está vazia.

## 6. Backlog priorizado (ordem sugerida de execução)

1. `UsuarioSistema` + `AppUserDetailsService` (segurança — risco mais alto).
2. `PageWrapper` (usado em todas as telas com paginação).
3. `VendasImpl` (`totalPorMes`/`totalPorOrigem`/`total`/`filtrar` — dashboard e relatórios).
4. `Venda` + `VendasController` (regra de negócio central da aplicação).
5. `FotoStorageS3` (integração externa — falhas silenciosas são caras).
6. `VendaListener` (efeito colateral crítico: baixa de estoque).
7. Padrão `equals`/`hashCode`/`toString` em todas as entidades (`com.brewer.model.*`), preferencialmente com `EqualsVerifier`.
8. Demais `repository.helper.*Impl` e `controller.validator`.

## 7. Critério de sucesso

- Mutation score global do módulo `com.brewer.*` (escopo analisado) subindo de **~61% para ≥ 80%**.
- Nenhum pacote com score abaixo de 60% (hoje: `security`, `controller.page`, `repository.helper.venda`, `service.event.venda`).
- Reexecutar `mvn -Dmaven.compiler.release=21 org.pitest:pitest-maven:mutationCoverage` após cada lote de testes novos e comparar `target/pit-reports/index.html` com o baseline deste documento.

## 8. Resultado pós-execução do plano

Após implementar os testes descritos nas seções 5.1 a 5.9 (incluindo `equalsverifier` como dependência de teste para as entidades de `com.brewer.model`), o plano foi reexecutado com `mvn -Dmaven.compiler.release=21 clean test-compile org.pitest:pitest-maven:mutationCoverage`:

| Métrica | Baseline | Após execução do plano |
|---|---:|---:|
| Mutantes gerados | 685 | 685 |
| Mutantes mortos (KILLED/TIMED_OUT) | 415 | **570** |
| **Mutation score** | **~61%** | **83%** |
| Cobertura de linha (classes mutadas) | 92% | 97% |
| Test strength | 73% | 89% |
| Testes executados | 159 classes | 275 testes (suíte `mvn test`) |

Critérios de sucesso da seção 7 atendidos:
- ✅ Mutation score global subiu de ~61% para **83%** (meta ≥80%).
- ✅ Nenhum pacote do escopo analisado ficou abaixo de 60% (pior caso: `com.brewer.repository.helper.estilo` e `com.brewer.repository.helper.usuario`, ambos com 66,7%).

Score por pacote após a execução (ordenado do menor para o maior):

| Pacote | Score |
|---|---:|
| `com.brewer.repository.helper.estilo` | 66,7% |
| `com.brewer.repository.helper.usuario` | 66,7% |
| `com.brewer.repository.helper.cliente` | 70,0% |
| `com.brewer.repository.helper.cidade` | 70,0% |
| `com.brewer.model` | 73,3% |
| `com.brewer.model.validation` | 80,0% |
| `com.brewer.storage.local` | 81,8% |
| `com.brewer.validation.validator` | 83,3% |
| `com.brewer.repository.helper.cerveja` | 86,4% |
| `com.brewer.service` | 89,5% |
| `com.brewer.storage.s3` | 90,0% |
| `com.brewer.session` | 90,5% |
| `com.brewer.controller.validator` | 92,9% |
| `com.brewer.controller.page` | 95,7% |
| `com.brewer.controller` | 97,1% |
| `com.brewer.controller.handler`, `com.brewer.repository.helper.venda`, `com.brewer.security`, `com.brewer.service.event.venda`, `com.brewer.storage`, `com.brewer.controller.converter` | 100% |

Destaques: `com.brewer.security` (13% → 100%), `com.brewer.controller.page` (26% → 95,7%), `com.brewer.repository.helper.venda` (35% → 100%) e `com.brewer.service.event.venda` (33% → 100%) — todos os pacotes 🔴 críticos do baseline saíram do estado crítico. `com.brewer.model` (maior volume de mutantes, 292) subiu de 51% para 73,3%; os repositórios `helper.*Impl` restantes (estilo, usuário, cliente, cidade) ficaram na faixa 66–70%, ainda com oportunidade de melhoria em um próximo ciclo, mas acima do piso de 60% exigido.
