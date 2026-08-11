# Status e Próximos Passos (Roadmap de Desenvolvimento)

Este arquivo é o **quadro oficial de acompanhamento do projeto**. Ele deve ser consultado e atualizado ao final de cada etapa ou sessão de trabalho por qualquer desenvolvedor ou assistente de IA.

---

## 🟢 1. O que já foi Implementado

- [x] Projeto Spring Boot criado (Java 25, Spring Boot 4.1, Maven, Flyway, Lombok, PostgreSQL driver, Spring Security, Bean Validation)
- [x] Migration `V1__criar_estrutura_inicial.sql` com tabelas base (`usuario`, `aportante`, `imovel`, `etapa_projeto`, `despesa`, `despesa_pagamento`)
- [x] Migration `V3__limpar_colunas_antigas.sql`
- [x] Pacote `shared/` com enums (`StatusImovel`, `TipoImovel`), exceptions (`RecursoNaoEncontradoException`, `RegraDeNegocioException`, `ApiErrorHandler`), config (`SecurityConfig`, `DatabaseCleanupRunner`)
- [x] CRUD completo de `Imovel` com Soft Delete (`ativo = false`)
- [x] CRUD completo de `Aportante` com Soft Delete (`ativo = false`)
- [x] CRUD completo de `EtapaProjeto` (Catálogo global reutilizável)
- [x] CRUD completo de `Despesa` com rateio via `DespesaPagamento` e validação de limite
- [x] **[Sprint 1]** Documentação do modelo de dados em `docs/MODELO-DADOS.md` com diagrama ER em Mermaid
- [x] **[Sprint 1]** Criação do `application.properties.example` e ajuste de `ddl-auto=update`
- [x] **[Sprint 1]** Implementação de alteração/edição completa em Despesas (`PUT /api/despesas/{id}`)
- [x] **[Sprint 1]** Suporte a filtros dinâmicos no endpoint `GET /api/despesas?imovelId={id}&etapaProjetoId={id}`
- [x] **[Sprint 1]** Migration `V4__criar_orcamento_etapa.sql` e pacote `orcamentoEtapa/` com cálculo em tempo real de total gasto, diferença e status (`NO_PRAZO`, `ATENCAO`, `ESTOURADO`)

---

## ⏳ 2. Roadmap do MVP (Mínimo Produto Viável)

### 📌 Etapa 5 — Armazenamento e Upload de Arquivos (Fotos & Comprovantes)
- [ ] Serviço em `shared/storage/` (Local / S3) para upload e download de arquivos
- [ ] Endpoints para anexo de fotos do imóvel (RF06 / Diário de Obra simplificado) e comprovantes de despesa (RF04)

### 📌 Etapa 6 — Módulo de Relatórios e Analytics (RF05)
- [ ] Pacote `relatorio/`: Endpoints agregados para:
  - Custo total acumulado por imóvel
  - Custo por metro quadrado (`R$/m²`)
  - Extrato acumulado de aportes por socio/investidor (`Aportante`)
  - Comparativo Orçado vs Realizado por etapa e total do imóvel

### 📌 Etapa 7 — Autenticação e Segurança JWT (RNF01)
- [ ] Pacote `auth/`: `UsuarioModel`, `UsuarioRepository`, `UsuarioService`, `AuthController` (`/api/auth/login`)
- [ ] Substituir HTTP Basic no `SecurityConfig` por filtro stateless JWT (`JwtFilter`) e permissões por Role (`ADMIN`, `USER`)

### 📌 Etapa 8 — Cobertura de Testes
- [ ] Testes unitários de serviços (`ImovelService`, `AportanteService`, `EtapaProjetoService`, `DespesaService`, `OrcamentoEtapaService`)
- [ ] Testes de integração de controllers com `MockMvc` / `TestRestTemplate`

### 📌 Etapa 9 — Frontend Responsivo (Angular 19+ & Angular Material)
- [ ] Setup do projeto Angular + Angular Material
- [ ] Autenticação (Tela de login + AuthGuard)
- [ ] Dashboard com KPIs financeiras (Custo Total, R$/m², Orçado vs Realizado)
- [ ] Telas CRUD (Imóveis, Aportantes, Catálogo de Etapas, Planejamento de Orçamento)
- [ ] Tela de Lançamento de Despesas no Canteiro de Obras (Mobile-friendly)

---

## 🔮 3. Fase 2 — Evoluções Futuras (Pós-MVP)

- [ ] Módulo `fornecedor/`: Cadastro completo de fornecedores, histórico de serviços e banco de orçamentos/cotações.
- [ ] Diário de Obra Avançado: Registro diário de clima, equipes, ocorrências e diário fotográfico com marcação no tempo.
- [ ] OCR de Notas Fiscais: Leitura automática de PDFs de NF-e/boletos via AI (Document AI / Textract).
- [ ] Alertas Proativos: Disparo automático de e-mails/push notifications ao atingir limites de orçamento.
- [ ] DRE Executiva e Projeção de Lucro (Lucro Esperado vs Realizado).
