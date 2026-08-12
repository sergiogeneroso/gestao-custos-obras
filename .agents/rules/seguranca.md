---
paths:
  - "src/main/resources/application*.properties"
  - "src/main/java/**/config/**"
---

# Segurança

- Nunca commitar credenciais reais em `application.properties`. Versionar
  apenas `application.properties.example` com placeholders (inclui
  `app.jwt.secret` — injetar via env em produção)
- Prefixo `/api/` em todos os endpoints REST
- `SecurityConfig` usa JWT stateless (jjwt): `/api/auth/login` público,
  demais endpoints exigem `Authorization: Bearer <token>`.
  `JwtService` (HMAC-SHA256) gera/valida tokens; `JwtAuthenticationFilter`
  extrai o `Authentication` (authorities `ROLE_<role>`);
  `JwtAuthenticationEntryPoint` devolve 401.
- Senha armazenada como hash BCrypt (`BCryptPasswordEncoder`). Seed admin
  inicial via `SeedUsuarioAdminRunner` (admin@gestao.local / admin123).
- A role viaja no token; regras `.hasRole(...)` por domínio pendentes para
  a fase 2 (quando os outros tipos de role forem definidos)
- Erros padronizados via `ApiErrorHandler`:
  `RecursoNaoEncontradoException` → 404, `RegraDeNegocioException` → 422,
  `MethodArgumentNotValidException` → 400; ausência/invalidade de
  autenticação → 401
