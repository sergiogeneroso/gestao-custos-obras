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

## Roadmap do MVP

### Etapa 7 — Autenticação JWT (RNF01)
- [ ] `auth/`: `UsuarioModel`, `UsuarioRepository`, `UsuarioService`,
      `AuthController` (`/api/auth/login`)
- [ ] Substituir HTTP Basic por filtro stateless JWT + roles (`ADMIN`, `USER`)

### Etapa 8 — Testes
- [ ] Testes unitários de cada Service
- [ ] Testes de integração de Controllers (`MockMvc`)

### Etapa 9 — Frontend Angular
- [ ] Setup Angular + Angular Material
- [ ] Login + AuthGuard
- [ ] Dashboard (Custo Total, R$/m², Orçado vs. Realizado)
- [ ] Telas CRUD (Imóveis, Aportantes, Etapas, Orçamento)
- [ ] Lançamento de despesas mobile-friendly (canteiro de obras)

## Pós-MVP (fase 2)

- [ ] `fornecedor/`: cadastro, histórico, banco de orçamentos
- [ ] Diário de obra (clima, equipe, ocorrências, fotos com timestamp)
- [ ] OCR de notas fiscais
- [ ] Alertas de orçamento (e-mail/push)
- [ ] DRE executiva / projeção de lucro
