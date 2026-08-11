# Modelo de Dados

Schema PostgreSQL gerenciado via Flyway (`src/main/resources/db/migration/`).
Para os campos exatos de cada tabela, ver a migration correspondente ou o
`Model.java` do domínio — não duplicado aqui de propósito.

## Diagrama ER

```mermaid
erDiagram
    USUARIO {
        BIGSERIAL id PK
        VARCHAR nome
        VARCHAR email UK
        VARCHAR senha_hash
        VARCHAR role
    }
    IMOVEL {
        BIGSERIAL id PK
        VARCHAR identificador
        VARCHAR tipo "CHECK (LOTE, IMOVEL)"
        VARCHAR status "PLANEJAMENTO, CONSTRUCAO, FINALIZADO"
        BOOLEAN ativo
    }
    APORTANTE {
        BIGSERIAL id PK
        VARCHAR nome
        VARCHAR tipo_participacao
        BOOLEAN ativo
    }
    ETAPA_PROJETO {
        BIGSERIAL id PK
        VARCHAR nome
    }
    ORCAMENTO_ETAPA {
        BIGSERIAL id PK
        BIGINT imovel_id FK
        BIGINT etapa_projeto_id FK
        NUMERIC valor_orcado "14,2"
    }
    DESPESA {
        BIGSERIAL id PK
        BIGINT imovel_id FK
        BIGINT etapa_projeto_id FK
        NUMERIC valor "14,2"
    }
    DESPESA_PAGAMENTO {
        BIGSERIAL id PK
        BIGINT despesa_id FK
        BIGINT aportante_id FK
        NUMERIC valor_pago "14,2"
    }
    IMOVEL_FOTO {
        BIGSERIAL id PK
        BIGINT imovel_id FK
        VARCHAR url
    }

    IMOVEL ||--o{ DESPESA : "possui"
    IMOVEL ||--o{ IMOVEL_FOTO : "galeria"
    ETAPA_PROJETO ||--o{ DESPESA : "classifica"
    IMOVEL ||--o{ ORCAMENTO_ETAPA : "planeja"
    ETAPA_PROJETO ||--o{ ORCAMENTO_ETAPA : "orça"
    DESPESA ||--o{ DESPESA_PAGAMENTO : "divide rateio"
    APORTANTE ||--o{ DESPESA_PAGAMENTO : "paga"
```

## Regras que não estão óbvias só olhando o schema

- `despesa_pagamento.aportante_id` → `ON DELETE RESTRICT`: impede apagar um
  aportante que já tem pagamento registrado (RNF02)
- `orcamento_etapa`: `UNIQUE(imovel_id, etapa_projeto_id)` — cada etapa só é
  orçada uma vez por imóvel
- Validação de negócio (não é constraint de banco, é no service):
  `SUM(despesa_pagamento.valor_pago) <= despesa.valor` — ver ADR-006 em
  `docs/DECISOES.md`
