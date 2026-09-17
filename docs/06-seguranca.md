# 06 · Segurança

[⬅ Voltar ao README](../Readme.md)

## Stack de segurança

O projeto usa **Spring Security 6** (via `spring-boot-starter-security`), configurado em
`com.brewer.config.SecurityConfig` no estilo moderno de `SecurityFilterChain` (component-based,
sem `WebSecurityConfigurerAdapter`, já removido no Spring Security 6).

## Autenticação

- **Form login** customizado, com página de login em `/login` (`SegurancaController` renderiza a
  tela; `formLogin(form -> form.loginPage(LOGIN).permitAll())`);
- Usuário autenticado é representado por `UsuarioSistema`, um `UserDetails` que encapsula a entidade
  de domínio `Usuario` + suas `GrantedAuthority`;
- `AppUserDetailsService implements UserDetailsService` busca o usuário por e-mail (apenas ativos) e
  monta a lista de permissões a partir dos grupos do usuário (`usuarios.permissoes(usuario)`);
- Senhas armazenadas com hash **BCrypt** (`BCryptPasswordEncoder`), usado tanto na autenticação
  (`DaoAuthenticationProvider`) quanto na definição de senha (`GeracaoDeSenha`).

## Autorização (RBAC)

- Modelo de **papéis dinâmicos**: cada `Usuario` pertence a um ou mais `Grupo`, e cada grupo agrega
  `Permissao`s (ex.: `CADASTRAR_CIDADE`, `CADASTRAR_USUARIO`) — carregadas do banco, não fixas em
  enum;
- Regras de autorização por rota, declaradas em `SecurityConfig`:
  - `/layout/**`, `/images/**` → público;
  - `/cidades/novo` → requer role `CADASTRAR_CIDADE`;
  - `/usuarios/**` → requer role `CADASTRAR_USUARIO`;
  - Qualquer outra rota → requer usuário autenticado;
- `@EnableMethodSecurity(prePostEnabled = true)` habilita autorização declarativa a nível de método
  (`@PreAuthorize`/`@PostAuthorize`) quando necessário além das regras de rota.

## Sessão

- **Sessão única por usuário**: `maximumSessions(1)` — um novo login invalida a sessão anterior;
- Sessão expirada ou inválida redireciona para `/login` (`invalidSessionUrl`/`expiredUrl`);
- O carrinho de itens de uma venda em andamento também vive na sessão HTTP
  (`TabelasItensSession`, `@SessionScope`) — ver [05 · Features](05-features.md#vendas).

## Boas práticas observadas no código

- Uso de `PathPatternRequestMatcher` (API moderna do Spring Security) para o matcher de logout;
- Separação clara entre entidade de domínio (`Usuario`) e adaptador de segurança (`UsuarioSistema`),
  evitando acoplar o modelo de negócio à API do Spring Security;
- Tratamento de acesso negado em fluxo de negócio (`AccessDeniedException` capturada em
  `VendasController#cancelar`, retornando página `403` amigável).

## Próxima leitura

- [02 · Arquitetura](02-arquitetura.md)
- [07 · Persistência e Dados](07-persistencia-e-dados.md)
