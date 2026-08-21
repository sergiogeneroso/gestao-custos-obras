---
paths:
  - "backend/src/main/resources/db/**"
---

# Banco de Dados e Migrations

- **Flyway está pausado (ADR-013)** enquanto a modelagem do MVP não fecha —
  `ddl-auto=update`, `FlywayConfig` sem `@Configuration`. **Não criar
  migrations novas** nesse meio-tempo: mude o `Model.java` e deixe o
  Hibernate aplicar direto. Quando o Flyway for reativado, as regras abaixo
  voltam a valer e a `V1` baseline é **escrita do zero** a partir do modelo
  final — as migrations históricas (V1, V3, V4 e V5) foram apagadas em Ago 2026
  justamente porque descreviam um schema que o reescopo já tinha substituído.
  Sobrou `V6__seed_usuario_admin.sql`, que não é schema e continua sendo o
  registro do usuário admin inicial
- Nomes de tabela e coluna em `snake_case`
- Foreign keys explícitas com `REFERENCES`; `ON DELETE RESTRICT` em relações
  financeiras (ex: `pagador_id` e `beneficiario_id` em `despesa`)
- Ao adicionar/alterar coluna, atualizar também `docs/MODELO-DADOS.md`
  (diagrama ER) na mesma tarefa
- (regras pré-pausa, valem de novo quando o Flyway for reativado) migrations
  versionadas em `backend/src/main/resources/db/migration/`; **nunca editar** uma já
  aplicada, sempre criar uma nova com o próximo número (`V7__...`, ...)

## `db/manual/` — o que o `ddl-auto=update` não faz

Com o Flyway pausado, o Hibernate cria tabela e coluna novas, mas **nunca**
apaga tabela ou coluna que saiu do modelo, não aplica `NOT NULL` em coluna que
já existe, não cria índice funcional e não migra dado nenhum. Tudo isso vira um
script em `backend/src/main/resources/db/manual/`, nomeado
`AAAA-MM-o-que-faz.sql`, que **alguém roda à mão uma vez** — o backend não o
executa.

Convenções desses scripts, seguidas pelos que já existem:

- Comentário no topo dizendo o porquê, a ADR relacionada e que é para rodar uma
  vez só
- Um `SELECT` de conferência **antes** da alteração destrutiva, e outro depois
  quando faz sentido
- O script fica no repositório mesmo depois de aplicado: é o registro da
  alteração, e outro ambiente ainda pode precisar dele
- Quando o Flyway voltar, o efeito acumulado desses scripts precisa estar dentro
  da `V1` baseline
