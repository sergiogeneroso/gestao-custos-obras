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

## Reescopo de Ago 2026 (ADR-019 a ADR-029)

O objetivo do projeto foi reescrito: de "gestão de despesas de obras" para
**resultado financeiro de cada imóvel do começo ao fim** (compra do lote, custos
do ciclo de vida, venda). O roadmap abaixo, das Etapas 7 a 9, reflete o escopo
antigo e permanece como registro do que já foi feito; o roadmap novo é este:

### Etapa A — Registro das decisões ✅
- [x] ADR-019 a ADR-029 em `docs/DECISOES.md`
- [x] Rules novas `ciclo-vida-imovel.md` e `contratos-financeiros.md`;
      `regras-negocio-financeiras.md` reescrita com a regra de custo
- [x] `AGENTS.md`, `CLAUDE.md`, `REQUISITOS.md`, `MODELO-DADOS.md` e
      `ARQUITETURA.md` atualizados

Cada etapa abaixo é retomável de forma independente, numa sessão própria: diz o
que ler antes, o que fazer e quando está pronta. As Etapas B a D mexem em código
que se referencia mutuamente, então, se forem feitas em sessões separadas, é
esperado que o backend só volte a compilar ao final da D.

### Etapa B — Backend: pessoa e fornecedor ✅
**Ler antes:** ADR-021, ADR-022.
- [x] Renomear o pacote `aportante/` → `pessoa/` (7 arquivos: Model, Repository,
      Service, Mapper, Controller e os dois DTOs); endpoint `/api/pessoas`
- [x] `PessoaModel`: `nome`, `tipoPessoa` (enum novo em `shared/enums/`),
      `documento` (único), `email`, `telefone`, `ativo`. Remover
      `tipoParticipacao`. Documento sem validação de dígito — comentário
      `ponytail:` no service
- [x] Novo domínio `fornecedor/` pela skill `gerar-crud-dominio`:
      `@OneToOne` para `PessoaModel` + `areaAtuacao`, `observacoes`, soft delete;
      endpoint `/api/fornecedores`

**Pronto quando:** `/api/pessoas` e `/api/fornecedores` respondem e nenhuma
classe referencia `Aportante`.

### Etapa C — Backend: despesa ✅
**Ler antes:** ADR-023, ADR-027, ADR-028 e
`.agents/rules/regras-negocio-financeiras.md`.
- [x] `DespesaModel`: `imovel` **opcional**; adicionar `pagador` (obrigatório),
      `beneficiario` (opcional), `faseImovel` (opcional — não existe para gasto
      geral), `contratoFinanceiro` (opcional) e `ativo`; trocar `etapaProjeto`
      por `categoriaDespesa`; remover `pagamentos` e `comprovanteUrl`
- [x] Apagar `DespesaPagamentoModel`, `DespesaPagamentoRepository` e os dois DTOs
      de pagamento; `DespesaService` perde todo o laço de rateio e a validação da
      soma; `deletar()` vira `inativar()`
- [x] `DespesaAnexoModel` tipado (COMPROVANTE/NOTA_FISCAL/RECIBO/CONTRATO/OUTRO),
      espelhando `ImovelFotoModel` e reusando `StorageService`; endpoints de
      upload, listagem por tipo e exclusão
- [x] Filtro de gastos gerais na listagem (`GET /api/despesas?semImovel=true`)

**Pronto quando:** dá para lançar despesa com pagador e beneficiário, anexar
comprovante e nota fiscal separadamente, e lançar despesa sem imóvel.

### Etapa D — Backend: categoria e imóvel ✅
**Ler antes:** ADR-020, ADR-024, ADR-026 e `.agents/rules/ciclo-vida-imovel.md`.
- [x] Renomear `etapaProjeto/` → `categoriaDespesa/` (endpoint
      `/api/categorias-despesa`) e `orcamentoEtapa/` → `orcamentoCategoria/`
- [x] Seed das categorias: Aquisição, ITBI/Escritura, Documentação, IPTU,
      Material, Mão de obra, Custos de financiamento, Corretagem, Impostos sobre
      a venda (via `CategoriaDespesaSeedRunner`, já que o Flyway está pausado)
- [x] `ImovelModel`: `tipo` → `fase` (`FaseImovel`) e `status` → `situacao`
      (`SituacaoImovel`); `dataInicioLote`, `dataInicioConstrucao`,
      `dataConclusaoObra`; `custoEstimadoObra`, `previsaoConclusao`;
      `@Embedded DadosCompra` e `DadosVenda`
- [x] `PATCH /api/imoveis/{id}/fase` e `/situacao` — só por aqui a fase e a
      situação mudam, porque gravam as datas de transição
- [x] `ImovelDocumentoModel` tipado, por fase, com endpoints próprios

**Pronto quando:** o backend compila, `./mvnw test` passa, e o ciclo
lote → construção → casa avança gravando as datas.

**Nota da implementação (Ago 2026):** a Etapa C depende de `categoriaDespesa/`
(renomeação da Etapa D) e de uma FK opcional para `contratoFinanceiro`
(domínio da Etapa E, ainda não implementada) — por isso o rename de
`etapaProjeto/` foi antecipado e `contratoFinanceiro/ContratoFinanceiroModel`
foi criado como **stub mínimo** (só `id`), só para a FK compilar; o CRUD
completo (tipo, contraparte, parcelas, quitação) continua sendo a Etapa E.
`relatorio/` também precisou de ajuste mínimo (não é a reescrita da Etapa F)
para voltar a compilar: `ExtratoAportanteDTO` virou `ExtratoPessoaDTO`
(usando `despesa.pagador` no lugar do `despesa_pagamento` removido) e os
parâmetros de etapa viraram `categoriaDespesaId`. O banco local foi recriado
do zero (`DROP SCHEMA public CASCADE`) porque tinha dados de teste residuais
que bloqueavam as colunas `NOT NULL` novas — isso apagou o usuário admin
seedado pela migration V6 (inerte com o Flyway pausado); rodar o `INSERT` do
`V6__seed_usuario_admin.sql` manualmente para voltar a logar.

### Etapa E — Backend: contratos financeiros ✅
**Ler antes:** ADR-025 e `.agents/rules/contratos-financeiros.md`.
- [x] `contratoFinanceiro/` pela skill `gerar-crud-dominio`:
      `ContratoFinanceiroModel` (imóvel, tipo, contraparte, valor contratado,
      situação, data e valor de quitação) e `ParcelaContratoModel` (número,
      vencimento, valor, `valorJuros`, data e valor de pagamento)
- [x] `quitar(id, data, valor)`: registra a quitação e encerra as parcelas em
      aberto **sem alterar os valores originais delas**
- [x] Aviso (sem bloqueio) ao iniciar construção com `PARCELAMENTO_COMPRA` ativo

**Pronto quando:** dá para encadear parcelamento da compra → quitação antecipada
→ financiamento de construção no mesmo imóvel.

**Nota da implementação (Ago 2026):** sem `DELETE`/soft delete em
`ContratoFinanceiro` — a ADR-028 lista soft delete só para `Imovel`, `Pessoa`,
`Fornecedor` e `Despesa`, e contrato nesse negócio só progride de `ATIVO` para
`QUITADO`, nunca é removido. `quitar()` só grava os três campos do contrato
(`situacao`, `dataQuitacao`, `valorQuitacao`) e não toca em nenhuma parcela —
"encerrar as parcelas em aberto" é tratado como leitura no relatório (parcela
com vencimento após a quitação deixa de contar como pendente), não como
mutação de dado, para respeitar a regra de nunca alterar valores originais. O
aviso de construção viaja como campo `aviso` (nullable) só na
`ImovelResponseDTO` devolvida por `PATCH /api/imoveis/{id}/fase` — as outras
respostas de imóvel continuam com `aviso: null`.

**Pronto quando:** dá para encadear parcelamento da compra → quitação antecipada
→ financiamento de construção no mesmo imóvel.

### Etapa F — Backend: relatórios
**Ler antes:** a regra de custo na ADR-025.
- [ ] `ExtratoAportanteDTO` → `ExtratoPessoaDTO` (mantendo filtro `> 0` e CSV `;`)
- [ ] `GET /api/relatorios/resultado-imovel`: despesas por fase, juros pagos,
      custo total, lucro, margem, tempo por fase, dias em carteira,
      rentabilidade anualizada, `resultadoProvisorio` e posição dos contratos
- [ ] `GET /api/relatorios/historico-fornecedor` e `GET /api/relatorios/carteira`
- [ ] Testes cobrindo: prestação não vira custo, saldo devedor não entra no
      custo, gasto geral não entra no custo de imóvel nenhum

**Pronto quando:** os testes da regra de custo passam.

### Etapa G — Frontend
- [ ] Renomear features placeholder (`aportantes/` → `pessoas/`,
      `etapas-projeto/` → `categorias-despesa/`, `orcamento-etapa/` →
      `orcamento-categoria/`), novas rotas `fornecedores/` e `contratos/`,
      ajustar `app.routes.ts`, o menu do `shell.ts` e os textos de landing/login
- [ ] `imovel.model.ts` e a tela de imóveis com fase, situação e compra/venda
- [ ] `dashboard.service.ts` passa a consumir `/api/relatorios/carteira` em vez
      de recalcular no frontend
- [ ] Telas CRUD de Pessoa, Fornecedor, Despesa e Contratos pela skill
      `gerar-crud-frontend`

### Etapa H — Schema e skills
- [ ] Recriar o banco local do zero — `ddl-auto=update` não remove tabelas
      antigas, e `aportante`, `despesa_pagamento` e `etapa_projeto` ficariam
      órfãs
- [ ] Atualizar os exemplos que citam `aportante`/`etapa` em
      `.agents/skills/gerar-crud-dominio/SKILL.md` e `gerar-crud-frontend/SKILL.md`
- [ ] Atualizar a estrutura de pastas em `docs/FRONTEND.md`

## Roadmap do MVP (escopo anterior, histórico)

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
- [x] Setup Angular + Angular Material
- [x] Estrutura de pastas (`core/`, `features/<domínio>/`, `shared/`) +
      roteamento base: `Shell` (toolbar/sidenav) em `core/layout/`, rotas
      lazy (`loadComponent`) por domínio em `app.routes.ts`, placeholders
      em `features/*`
- [x] Login + AuthGuard: `core/auth/` (`AuthService` com signal de usuário +
      `localStorage`, `authInterceptor` funcional injeta Bearer,
      `authGuard` protege o `Shell`), tela de login split-screen
      (`core/auth/login/`). Testado ponta a ponta com o seed admin
      (login → área logada → logout)
- [x] Design system Nocturne adotado como tema default (ADR-018): paleta,
      Inter, ícones Phosphor, raio/densidade globais, landing page pública
      em `/`, `Shell` movido pra `/painel`
- [x] Dashboard (`features/dashboard/`): KPIs (custo lançado no mês, custo
      médio por m², despesas sem comprovante) e 3 imóveis mais recentes,
      alternável cards/lista — dados montados no frontend a partir de
      `GET /api/imoveis` + `GET /api/despesas`, sem endpoint novo. Gráficos
      (Custo Total, Orçado vs. Realizado, ADR-017/Chart.js) ainda pendentes
- [ ] Telas CRUD (Imóveis, Aportantes, Etapas, Orçamento)
- [ ] Lançamento de despesas mobile-friendly (canteiro de obras)
- [ ] RF07 — tema configurável: endpoint de config no backend
      (`ROLE_ADMIN`), 5 paletas curadas (Nocturne dark-only + 4 com par
      light/dark, ADR-016/ADR-018), painel admin no frontend pra trocar

## Módulos pós-MVP (ADR-029)

- [ ] Orçamento por categoria (`orcamentoCategoria/` já existe no código, mas
      está fora do MVP)
- [ ] Cotações e banco de orçamentos de fornecedor — o **cadastro** de fornecedor
      entrou no núcleo (RF08); só as cotações ficaram para depois
- [ ] Diário de obra (clima, equipe, ocorrências, fotos com timestamp)
- [ ] OCR de notas fiscais
- [ ] Alertas de orçamento e de parcelas a vencer (e-mail/push)
- [ ] Trilha de auditoria (quem alterou o quê e quando)
- [ ] Reativação do Flyway com baseline V1 única — risco registrado na ADR-029:
      fica mais caro depois do primeiro imóvel real lançado
