---
paths:
  - "backend/src/main/resources/application*.properties"
  - "backend/src/main/java/**/config/**"
---

# Segurança

- Nunca commitar credenciais reais em `application.properties`. Versionar
  apenas `application.properties.example` com placeholders (inclui
  `app.jwt.secret` — injetar via env em produção)
- Prefixo `/api/` em todos os endpoints REST
- `SecurityConfig` usa JWT stateless (jjwt): `/api/auth/login` público,
  demais endpoints exigem `Authorization: Bearer <token>`.
  `JwtService` (HMAC-SHA256) gera/valida tokens carregando só a identidade
  (e-mail), sem authorities — a autorização não viaja no token, é resolvida
  a cada requisição (ver RBAC abaixo). `JwtAuthenticationEntryPoint` devolve
  401.
- Senha armazenada como hash BCrypt (`BCryptPasswordEncoder`). Não existe
  seed runner para `usuario` — o usuário inicial é inserido à mão no banco
  de dev; `PerfilSeedRunner` só garante o perfil "Administrador" e migra
  usuários sem perfil pra ele.
- **RBAC por domínio (ADR-046), implementado:** matriz configurável em
  banco (`Perfil` × `PerfilPermissao`), não papéis fixos hardcoded.
  `UsuarioModel.perfil` (FK para `PerfilModel`, único por usuário) substitui
  o antigo campo `role` (String). Domínios são um enum fixo no código
  (`DominioSistema`: `imovel`, `pessoa`, `despesa`, `contratoFinanceiro`,
  `categoriaDespesa`, `relatorio`, `auditoria`, `perfil`) — só a matriz é
  editável em runtime, não a lista de domínios. Quatro ações por domínio
  (`AcaoPermissao`): `acessar`/`incluir`/`alterar`/`deletar`, sem noção de
  dono do registro; `acessar` é pré-requisito das outras três na mesma
  linha da matriz (`PerfilRequestDTO.isAcessarPrerequisitoDasDemais`).
  Enforcement via `PermissaoInterceptor` (`HandlerInterceptor`, registrado
  em `WebMvcConfig`), que deriva domínio do path (`/api/despesas/**` →
  despesa) e ação do verbo HTTP (GET→acessar, POST→incluir, PUT/PATCH→
  alterar, DELETE→deletar) — sem anotação por endpoint; falta de permissão
  lança `AcessoNegadoException` (403 via `ApiErrorHandler`). `perfil` é só
  mais um domínio da própria matriz (gerenciar perfis exige "alterar" em
  "perfil"), sem trava de "último admin". `auth`, `arquivos` e
  `orcamentos-categoria` ficam fora do RBAC (só autenticação). Qualquer
  usuário autenticado consulta `GET /api/perfis/minhas-permissoes` (as
  PRÓPRIAS permissões), rota excluída da checagem geral do domínio
  "perfil" — é o que o frontend consulta pra montar a UI. Vocabulário
  completo em `CONTEXT.md`; frontend (telas + guards/diretivas) ainda
  pendente.
- Erros padronizados via `ApiErrorHandler`:
  `RecursoNaoEncontradoException` → 404, `RegraDeNegocioException` → 422,
  `MethodArgumentNotValidException` → 400; ausência/invalidade de
  autenticação → 401
