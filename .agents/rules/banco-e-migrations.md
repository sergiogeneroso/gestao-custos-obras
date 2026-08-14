---
paths:
  - "backend/src/main/resources/db/migration/**"
---

# Banco de Dados e Migrations

- **Flyway está pausado (ADR-013)** enquanto a modelagem do MVP não fecha —
  `ddl-auto=update`, `FlywayConfig` sem `@Configuration`. **Não criar
  migrations novas** nesse meio-tempo: mude o `Model.java` e deixe o
  Hibernate aplicar direto. Quando o Flyway for reativado, as regras abaixo
  voltam a valer e as migrations antigas em `db/migration/` são consolidadas
  numa `V1` baseline única.
- Nomes de tabela e coluna em `snake_case`
- Foreign keys explícitas com `REFERENCES`; `ON DELETE RESTRICT` em relações
  financeiras (ex: `pagador_id` e `beneficiario_id` em `despesa`)
- Ao adicionar/alterar coluna, atualizar também `docs/MODELO-DADOS.md`
  (diagrama ER) na mesma tarefa
- (regras pré-pausa, valem de novo quando o Flyway for reativado) migrations
  versionadas em `backend/src/main/resources/db/migration/`; **nunca editar** uma já
  aplicada, sempre criar uma nova com o próximo número (`V7__...`, ...)
