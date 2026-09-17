# 07 · Persistência e Dados

[⬅ Voltar ao README](../Readme.md)

## Camada de persistência

- **Spring Data JPA** para o CRUD básico: interfaces como `Cervejas`, `Clientes`, `Usuarios`,
  `Vendas`, `Grupos`, `Estados` estendem os repositórios Spring Data e ganham métodos como
  `save`, `findById`, `deleteById` automaticamente;
- **Consultas dinâmicas customizadas** via padrão *Repository + Helper/Impl*: cada agregado que
  precisa de filtros complexos (pesquisa combinável, relatórios agregados) declara uma interface
  `*Queries` (ex.: `VendasQueries`) e uma implementação `*Impl` (ex.: `VendasImpl`) que usa
  **JPA `EntityManager`/`TypedQuery`** diretamente para montar a query em tempo de execução — útil
  quando os filtros são opcionais e não dá para usar apenas *derived query methods*;
- Pacote `repository.filter` concentra os **objetos de filtro** usados nas telas de pesquisa
  (`VendaFilter`, `ClienteFilter`, `CervejaFilter`, `CidadeFilter`, `EstiloFilter`, `UsuarioFilter`).

## Migrações de banco (Flyway)

- Scripts versionados em `src/main/resources/db/migration`, nomeados `V<n>__descricao.sql`
  (ex.: `V01__criar_tabelas_estilo_e_cerveja.sql` até `V14__remover_display_width_e_converter_utf8mb4.sql`);
- O Flyway aplica automaticamente as migrações pendentes na inicialização da aplicação, garantindo
  que o schema do MySQL esteja sempre sincronizado com o código;
- Cobrem toda a evolução do schema: criação das tabelas de domínio, controle de estoque, fotos,
  endereço/cidade/estado, clientes, RBAC (usuário/grupo/permissão) e vendas/itens de venda.

## Paginação

- `com.brewer.repository.paginacao.PaginacaoUtil`: aplica `firstResult`/`maxResults` em queries
  JPA cruas (Criteria/`TypedQuery`) a partir de um `Pageable` do Spring Data;
- `com.brewer.controller.page.PageWrapper`: decora um `Page<T>` do Spring Data para expor,
  nas views Thymeleaf, informações prontas para montar os controles de paginação (página atual,
  total de páginas, é primeira/última, URL da próxima página, etc.).

## Cache

- **EhCache** (via JCache/`javax.cache`) disponível como provedor de cache para reduzir consultas
  repetidas ao banco em dados pouco voláteis (ex.: listas de apoio como estilos/estados).

## Modelo de domínio (destaques)

- `BaseEntity`: superclasse comum das entidades com atributos/comportamento compartilhado;
- `Venda` + `ItemVenda`: agregam a lógica de cálculo de valor total (subtotal + frete − desconto) e
  de associação bidirecional item→venda;
- `Usuario` + `Grupo` + `UsuarioGrupo` (chave composta `UsuarioGrupoId`) + `Permissao`: modelam o
  RBAC do sistema (ver [06 · Segurança](06-seguranca.md));
- `Cidade` + `Estado` + `Endereco`: dados de localização usados por `Cliente`;
- Enums de domínio: `StatusVenda`, `TipoPessoa`, `Origem`, `Sabor`.

## Próxima leitura

- [02 · Arquitetura](02-arquitetura.md)
- [09 · Qualidade e Testes](09-qualidade-e-testes.md)
