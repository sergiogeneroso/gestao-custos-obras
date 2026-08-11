---
paths:
  - "src/main/resources/application*.properties"
  - "src/main/java/**/config/**"
---

# Segurança

- Nunca commitar credenciais reais em `application.properties`. Versionar
  apenas `application.properties.example` com placeholders
- Prefixo `/api/` em todos os endpoints REST
- `SecurityConfig` atual usa HTTP Basic stateless (temporário) — será
  substituído por JWT no módulo `auth/` (RNF01, ainda não implementado)
- Erros padronizados via `ApiErrorHandler`:
  `RecursoNaoEncontradoException` → 404, `RegraDeNegocioException` → 422,
  `MethodArgumentNotValidException` → 400
