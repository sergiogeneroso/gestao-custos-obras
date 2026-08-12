# Status e Próximos Passos

Consultar e atualizar ao final de cada sessão de trabalho.

## Feito

- Base: Spring Boot 4.1 (Java 25), Flyway, Security, Bean Validation
- CRUD completo: `imovel` (+ galeria de fotos), `aportante`, `etapaProjeto`,
  `despesa` (+ rateio, PUT, filtros, upload de comprovante), `orcamentoEtapa`
  (orçado vs. realizado)
- `relatorio/`: custo por imóvel/m², extrato de aportantes, orçado vs.
  realizado, exportação CSV (RF05)
- `shared/storage`: upload/download local, extensível para cloud
- `application.properties.example` criado; `ddl-auto=update`
- `auth/`: domínio de autenticação — `UsuarioModel`, `UsuarioRepository`,
  `AuthService`, `AuthController` (`/api/auth/login`), login emite JWT
- `shared/config/JwtService` + `JwtAuthenticationFilter` (stateless, jjwt),
  `BCryptPasswordEncoder`, `JwtAuthenticationEntryPoint` (401 adequado)
- HTTP Basic removido; substituído por JWT (RNF01)
- Seed admin inicial via migration V6 (BCrypt) — admin@gestao.local / admin123
- Flyway restaurado como única fonte de schema (`FlywayConfig` `@Bean`
  manual): o Spring Boot 4.1 removeu a `FlywayAutoConfiguration`, então
  `ddl-auto=validate` e `migrate()` roda por bean antes do EMF (ADR-012).
  `DatabaseCleanupRunner` removido (era para limpar pre-ADR-003/004).
- Flyway **pausado** (ADR-013) até a modelagem do MVP fechar: `ddl-auto=update`,
  `FlywayConfig` sem `@Configuration`, schema direto pelo Hibernate

## Roadmap do MVP

### Etapa 7 — Autenticação JWT (RNF01) ✅
- [x] `auth/`: `UsuarioModel`, `UsuarioRepository`, `AuthService`,
      `AuthController` (`/api/auth/login`)
- [x] Substituir HTTP Basic por filtro stateless JWT + role no token
      (`ROLE_<role>` no `Authentication`; regras `.hasRole(...)` por
      domínio ficam pendentes para a fase 2, quando os outros tipos de
      role forem definidos)

### Etapa 8 — Testes
- [ ] Testes unitários de cada Service
- [ ] Testes de integração de Controllers (`MockMvc`)

### Etapa 9 — Frontend Angular
- [ ] Setup Angular + Angular Material
- [ ] Login + AuthGuard
- [ ] Dashboard (Custo Total, R$/m², Orçado vs. Realizado)
- [ ] Telas CRUD (Imóveis, Aportantes, Etapas, Orçamento)
- [ ] Lançamento de despesas mobile-friendly (canteiro de obras)
- [ ] RF07 — tema configurável: endpoint de config no backend
      (`ROLE_ADMIN`), 4 paletas curadas + light/dark (ADR-016), painel admin
      no frontend pra trocar

## Pós-MVP (fase 2)

- [ ] `fornecedor/`: cadastro, histórico, banco de orçamentos
- [ ] Diário de obra (clima, equipe, ocorrências, fotos com timestamp)
- [ ] OCR de notas fiscais
- [ ] Alertas de orçamento (e-mail/push)
- [ ] DRE executiva / projeção de lucro
