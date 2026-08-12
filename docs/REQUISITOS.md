# Requisitos

## Requisitos Funcionais (RF)

### RF01 — Imóveis/Lotes ✅
Campos: identificador único, tipo (Lote/Imóvel), endereço, área (m²), valor
de aquisição inicial, status (Planejamento/Construção/Finalizado), descrição.
Ações: criar, listar, editar, inativar (soft delete). `/api/imoveis`

### RF02 — Aportantes ✅
Pessoas físicas/jurídicas que aportam recursos. Campos: nome, documento
(CPF/CNPJ), e-mail, telefone, tipo de participação. Ações: criar, listar,
editar, inativar (soft delete). `/api/aportantes`

### RF03 — Etapas do Projeto ✅
Catálogo global reutilizável (Aquisição do Terreno, Documentação, Fundação,
Alvenaria, Acabamento...). Ações: criar, listar, editar, excluir.
`/api/etapas-projeto`

### RF04 — Despesas ✅
Valor, data, descrição, comprovante, etapa e imóvel vinculados. Rateio entre
1+ aportantes via `DespesaPagamento`. Validação: soma dos pagamentos não
excede o valor da despesa. `/api/despesas`

### RF05 — Relatórios ✅
Filtros: imóvel, período, etapa, aportante. Endpoints em `/api/relatorios`:
`custo-por-imovel`, `custo-por-m2`, `extrato-aportantes`,
`orcado-vs-realizado`. Exportação CSV via `?format=csv` (delimitador `;`,
compatível com Excel pt-BR). Gráficos ficam por conta do frontend Angular.

### RF06 — Upload de Imagens e Comprovantes ✅
Galeria de fotos do imóvel (`/api/imoveis/{id}/fotos`), comprovante de
despesa (`/api/despesas/{id}/comprovante`). Storage local
(`shared/storage/LocalStorageService`), extensível para S3/Cloud Storage.

## Requisitos Não Funcionais (RNF)

- **RNF01 — Autenticação** ✅: JWT stateless (jjwt). Login em `/api/auth/login`
  emite token; `JwtAuthenticationFilter` valida `Authorization: Bearer`.
  HTTP Basic removido. Senha armazenada como hash BCrypt. Role viaja no token;
  regras `.hasRole(...)` por domínio pendentes (fase 2).
- **RNF02 — Persistência** ✅: PostgreSQL, integridade referencial
  (`ON DELETE RESTRICT` em relações financeiras), Flyway.
- **RNF03 — Responsividade** ⏳ Pendente: Angular + Angular Material,
  mobile-friendly para lançamento de despesas no canteiro de obras.
- **RNF04 — Precisão Monetária** ✅: `BigDecimal` + `NUMERIC(14,2)`.
