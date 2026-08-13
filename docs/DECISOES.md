# Decisões Arquiteturais (ADRs)

## ADR-001 — Package by feature
Com 6+ domínios, organizar por camada espalharia cada funcionalidade por
4-5 pastas. Decisão: package by feature (vertical slice), `shared/` como
exceção para código transversal.

## ADR-002 — Soft delete para Imóvel e Aportante
Imóveis e aportantes podem ter despesas/pagamentos vinculados; DELETE físico
quebraria integridade referencial. Decisão: campo `ativo BOOLEAN`, listagens
via `findByAtivoTrue()`, DELETE no controller chama `inativar()`.

## ADR-003 — Nome definitivo: Aportante (não Envolvido)
"Envolvido" era genérico demais. "Aportante" reflete quem aporta recurso
financeiro — termo já usado no requisito RF05.

## ADR-004 — Nome definitivo: EtapaProjeto (não Etapa)
"Etapa" colide com nomes comuns em Java e é ambíguo. "EtapaProjeto" evita a
ambiguidade sem restringir a fases só de construção.

## ADR-005 — EtapaProjeto como catálogo global
Etapas se repetem em todos os imóveis. `etapa_projeto` é tabela independente,
sem FK para `imovel`. Trade-off aceito: não é possível ter etapa exclusiva
de um imóvel.

## ADR-006 — DespesaPagamento separado de Despesa
RF04 exige tanto "um aportante pagou tudo" quanto rateio entre sócios.
Tabela `despesa_pagamento` com FK para `despesa` e `aportante`; soma de
`valor_pago` não pode exceder `despesa.valor` (validado no service).

## ADR-007 — BigDecimal para valores monetários
`double`/`float` causam erro de arredondamento inaceitável para dinheiro.
`BigDecimal` (Java) + `NUMERIC(14,2)` (PostgreSQL); proibidos para dinheiro.

## ADR-008 — Mapper manual (sem MapStruct)
Projeto pequeno, mapeamentos simples — MapStruct adicionaria dependência e
complexidade de build desnecessárias. Pode migrar depois se a complexidade
crescer.

## ADR-009 — Angular Material para o frontend
Framework oficial do Angular, bem mantido, boa integração. Alternativas
avaliadas: PrimeNG, TailwindCSS, Bootstrap.

## ADR-010 — Flyway para migrações
Controle de versão do schema. Migrations em
`backend/src/main/resources/db/migration/`; nunca editar uma já aplicada.

## ADR-011 — Documentação dividida por ferramenta (Ago 2026)
Projeto usa Claude Code e Antigravity. Decisão: `AGENTS.md` na raiz como
fonte única de regras cross-tool; regras específicas por caminho em
`.agents/rules/` (symlink em `.claude/rules/`); skills em `.agents/skills/`
(symlink em `.claude/skills/`); `docs/` para referência sob demanda, nunca
importada via `@` no CLAUDE.md (importar carrega no contexto toda sessão,
o que anula o ganho de token).

## ADR-012 — Flyway por @Bean manual no Spring Boot 4.1 (Ago 2026)
O Spring Boot 4.1 removeu a `FlywayAutoConfiguration` (e `FlywayProperties`)
do `spring-boot-autoconfigure` — não há mais starter nem auto-config para
Flyway; só o BOM gerencia a versão do `flyway-core`. Consequência: ter
`flyway-core` no pom.xml **não** roda migrações nem cria
`flyway_schema_history` — o schema fica só por conta do Hibernate.
Decisão: declarar `Flyway` via `@Bean` em `shared/config/FlywayConfig.java`,
rodar `migrate()` num bean que o `entityManagerFactory` dependa (via
`BeanDefinitionRegistryPostProcessor`) e usar `ddl-auto=validate` (o Flyway
passa a ser a única fonte de schema). A recriação do DB aplicou V1–V6 limpas
(oportunidade para eliminar o `DatabaseCleanupRunner`, que visava limpar
mudanças pré-ADR-003/004). Upgrade:
se o Spring Boot reintroduzir auto-config/starter Flyway, remover
`FlywayConfig` e `spring.flyway.enabled=false` voltando à config nativa.
**Pausado por ADR-013** — ver abaixo; o desenho continua válido para quando
for reativado.

## ADR-013 — Pausa do Flyway durante a modelagem do MVP (Ago 2026)
Projeto em fase inicial, uso solo, sem dado relevante em disco, e a
modelagem de dados do MVP ainda não fechou. Escrever uma migration a cada
ajuste de coluna nesse estágio é atrito pago duas vezes: uma vez agora, outra
no squash quando o modelo estabilizar. Decisão: pausar o Flyway (`FlywayConfig`
sem `@Configuration`, `ddl-auto=update` no lugar de `validate`) e deixar o
Hibernate gerir o schema direto pelas entidades enquanto a modelagem estiver
em fluxo. As migrations em `db/migration/` ficam inertes, não são apagadas.
Reativar (religar `@Configuration`, voltar `ddl-auto=validate`, consolidar as
migrations antigas numa `V1` baseline única refletindo o modelo final) antes
de existir dado real que importe ou mais alguém mexendo no projeto —
retrofitar migrations num banco já povoado é bem mais caro que começar com
elas desde o primeiro dado real.

## ADR-014 — Frontend no mesmo repositório, Angular standalone sem NgRx (Ago 2026)
Decisões tomadas antes de iniciar a geração do frontend (Etapa 9 do roadmap).
**Localização:** `frontend/` na raiz do mesmo repositório (monorepo), em vez
de repositório separado — projeto solo, um só clone/histórico é mais simples
de manter do que sincronizar dois repositórios. **Standalone components:**
Angular moderno abandonou `NgModule` como padrão desde a v17; manter módulos
aqui seria carregar boilerplate que o próprio framework já descontinuou.
**Sem NgRx:** avaliado e descartado pelo mesmo raciocínio do ADR-008
(MapStruct) — projeto pequeno, CRUDs simples, estado local via `services` +
Angular `signals` resolve sem a complexidade de um store dedicado. Detalhes
de estrutura de pastas, autenticação (JWT em `localStorage`, interceptor
funcional, `authGuard`) e design (tema Material customizado, listagem de
imóveis em cards com foto de capa, desktop-first): `docs/FRONTEND.md`.

## ADR-015 — Backend movido para `backend/`, monorepo simétrico (Ago 2026)
O repositório nasceu só como backend (Spring Boot na raiz: `pom.xml`, `src/`,
`mvnw`). Ao decidir por `frontend/` na raiz (ADR-014), a estrutura ficou
assimétrica — arquivos do backend soltos na raiz, frontend contido numa
pasta. Avaliado o custo de mover agora (rename de ~80 arquivos rastreados,
mais ~15 referências de path em `.agents/rules/`, `docs/` e na skill
`gerar-crud-dominio`) contra o custo de mover depois, com o projeto maior:
decisão foi pagar o custo agora. Backend movido para `backend/` (git detectou
os renames automaticamente, histórico preservado); `frontend/` já nasce no
lugar certo quando for gerado. Comando de teste passa a ser
`cd backend && ./mvnw test`. Efeito colateral descoberto durante a migração:
`backend/src/main/resources/application.properties` estava — e continua —
rastreado no git desde o commit inicial, contrariando a própria regra de
`.agents/rules/seguranca.md`; risco baixo (credenciais são só de dev local:
`postgres`/`admin` em `localhost`, secret JWT com sufixo `dev-only`), mas
fica registrado aqui para decisão futura sobre destrackear/purgar do
histórico.

## ADR-016 — Tema configurável via painel admin, paletas curadas (Ago 2026)
RF07: admin pode trocar a paleta de cores do sistema, valendo pra todos os
usuários. **Persistência:** backend (não `localStorage`) — se ficasse só no
navegador de quem trocou, os demais usuários não veriam a mudança; precisa de
um endpoint de configuração simples (par chave-valor, não é um domínio CRUD
completo). **Mecanismo:** paletas pré-definidas (curadas), não color picker
livre — evita admin quebrar contraste/acessibilidade do tema Material sem
querer. Quatro paletas fechadas nesta sessão (hex e detalhes em
`docs/FRONTEND.md`): Azul corporativo (default), Terracota industrial, Verde
financeiro, Grafite + âmbar — cada uma com light e dark mode (ADR-014 já
previa toggle claro/escuro). **Efeito colateral:** esse endpoint de config
precisa ser admin-only, o que antecipa uma fatia pequena da RBAC que RNF01
tinha deixado pra fase 2 (`.hasRole("ADMIN")` só nesse endpoint específico,
não a regra geral por domínio). Ver ADR-018 — Nocturne entra como paleta
default, ocupando o lugar do Azul corporativo.

## ADR-017 — Tipografia Inter e ícones Lucide (Ago 2026)
Continuação das decisões de design visual (após ADR-016). **Tipografia:**
Inter no lugar da Roboto padrão do Material — sans-serif com boa legibilidade
em tabelas de números (uso pesado no projeto: valores monetários, m²),
comum em produtos financeiros/SaaS. Self-hosted via `@fontsource/inter` em
vez de Google Fonts via CDN, mesma lógica de não depender de terceiro em
runtime. **Ícones:** Lucide no lugar do Material Icons/Symbols — set mais
moderno/minimalista, via `lucide-angular` (wrapper oficial). Convive com
`mat-icon` só onde componentes do Material exigem ícone Material
internamente; nas telas de domínio o uso é direto via `lucide-angular`.
**Gráficos:** Chart.js direto, sem wrapper Angular (`ng2-charts`,
`ngx-echarts`) — os três gráficos do dashboard (RF05: Custo Total, R$/m²,
Orçado vs. Realizado) são simples o bastante pra não justificar mais uma
camada de integração; usado via `ViewChild` num `<canvas>`. Detalhes:
`docs/FRONTEND.md`. Ver ADR-018 — ícones migram de Lucide para Phosphor.

## ADR-018 — Adoção do design system Nocturne como tema default (Ago 2026)

Um design system chamado **Nocturne** foi importado via claude_design MCP
com templates já desenhados especificamente para este domínio (login,
dashboard interno, landing) — mais específico e completo do que a referência
de layout genérica (Berry/Flat Able/Gradient Able) usada até então. Emenda
pontos do ADR-009/016/017 sem invalidá-los:

**Paleta (emenda ADR-016):** Nocturne passa a ser a paleta **default**,
no lugar de Azul corporativo — que continua existindo como opção curada
normal para quando RF07 (troca de paleta pelo admin) for implementado.
Nocturne é um tema fixo dark-only (bg `#161826`, surface `#232532`, texto
`#e9e9ed`, accent `#9184d9`) — exceção documentada à regra do ADR-016 de
"cada paleta precisa de par light/dark": não existe variante light
desenhada para o Nocturne, então selecioná-lo no futuro painel admin
implica dark mode.

**Ícones (emenda ADR-017):** Phosphor substitui Lucide. Sem wrapper Angular
oficial (ao contrário do `lucide-angular`) — usado direto via classe CSS do
pacote `@phosphor-icons/web`.

**Forma e tipografia:** raio de 8px, densidade compacta e botões primários
sempre outline (nunca preenchidos) passam a ser a linha de base do tema
Material — como o Material só expõe cor por paleta (não forma/densidade),
esses três aspectos são globais, independentes de qual paleta de cor está
ativa. Tipografia Inter do ADR-017 (nunca chegou a ser implementada) entra
junto.

**Componentes (ADR-009 não muda):** Angular Material continua sendo o
framework de componentes. A cor exata do Nocturne é aplicada via override
dos tokens de sistema M3 (`--mat-sys-*`), não por paleta nomeada do
Material — nenhuma das paletas nomeadas (`$violet-palette` etc.) bate com a
saturação baixa do accent do Nocturne.

**Escopo desta sessão:** aplicado na tela de login, no shell interno (RF05
Dashboard incluído) e numa landing page nova antes do login — RF07 (troca
de paleta em runtime) continua pendente, sem mudança nesta ADR.
