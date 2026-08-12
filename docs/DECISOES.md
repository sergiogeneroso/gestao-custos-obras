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
`src/main/resources/db/migration/`; nunca editar uma já aplicada.

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
