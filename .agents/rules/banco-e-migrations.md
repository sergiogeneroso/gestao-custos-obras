---
paths:
  - "src/main/resources/db/migration/**"
---

# Banco de Dados e Migrations

- Flyway, migrations versionadas em `src/main/resources/db/migration/`
- **Nunca editar** uma migration já aplicada; sempre criar uma nova com o
  próximo número (`V4__...`, `V5__...`)
- Nomes de tabela e coluna em `snake_case`
- Foreign keys explícitas com `REFERENCES`; `ON DELETE RESTRICT` em relações
  financeiras (ex: `aportante_id` em `despesa_pagamento`)
- Ao adicionar/alterar coluna, atualizar também `docs/MODELO-DADOS.md`
  (diagrama ER) na mesma tarefa
