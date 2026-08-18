# Modelo de Dados

Schema PostgreSQL gerido pelo Hibernate enquanto o Flyway estiver pausado
(ADR-013/029). Para os campos exatos de cada tabela, ver o `Model.java` do
domínio — não duplicado aqui de propósito.

Este é o modelo **alvo** do reescopo de Ago 2026 (ADR-019 a ADR-029); a
implementação avança por fases.

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
    PESSOA {
        BIGSERIAL id PK
        VARCHAR nome
        VARCHAR tipo_pessoa "FISICA, JURIDICA"
        VARCHAR documento UK "CPF ou CNPJ"
        BOOLEAN ativo
    }
    FORNECEDOR {
        BIGSERIAL id PK
        BIGINT pessoa_id FK
        VARCHAR area_atuacao
        BOOLEAN ativo
    }
    IMOVEL {
        BIGSERIAL id PK
        VARCHAR identificador
        VARCHAR fase "LOTE, CONSTRUCAO, CASA"
        VARCHAR situacao "ADQUIRIDO, A_VENDA, VENDIDO"
        NUMERIC area_lote "10,2"
        NUMERIC area_construida "10,2"
        DATE data_inicio_lote
        DATE data_inicio_construcao
        DATE data_conclusao_obra
        NUMERIC custo_estimado_obra "14,2"
        DATE previsao_conclusao
        NUMERIC compra_valor "14,2"
        DATE compra_data
        BIGINT compra_vendedor_id FK
        NUMERIC venda_valor "14,2"
        DATE venda_data
        BIGINT venda_comprador_id FK
        NUMERIC venda_valor_pretendido "14,2"
        BOOLEAN ativo
    }
    CATEGORIA_DESPESA {
        BIGSERIAL id PK
        VARCHAR nome
    }
    CONTRATO_FINANCEIRO {
        BIGSERIAL id PK
        BIGINT imovel_id FK
        BIGINT contraparte_id FK
        VARCHAR tipo "PARCELAMENTO_COMPRA, FINANCIAMENTO_CONSTRUCAO, PARCELAMENTO_VENDA"
        NUMERIC valor_contratado "14,2"
        VARCHAR situacao "ATIVO, QUITADO"
        DATE data_quitacao
        NUMERIC valor_quitacao "14,2"
    }
    PARCELA_CONTRATO {
        BIGSERIAL id PK
        BIGINT contrato_id FK
        INT numero
        DATE data_vencimento
        NUMERIC valor "14,2"
        NUMERIC valor_juros "14,2"
        DATE data_pagamento "nula = em aberto"
        NUMERIC valor_pago "14,2 — pode diferir do valor contratado da parcela"
    }
    DESPESA {
        BIGSERIAL id PK
        BIGINT imovel_id FK "nulo = gasto geral"
        BIGINT categoria_despesa_id FK
        BIGINT pagador_id FK
        BIGINT beneficiario_id FK "opcional"
        BIGINT contrato_financeiro_id FK "opcional"
        VARCHAR fase_imovel "fase em que foi incorrida"
        NUMERIC valor "14,2"
        DATE data_pagamento
        BOOLEAN ativo
    }
    DESPESA_ANEXO {
        BIGSERIAL id PK
        BIGINT despesa_id FK
        VARCHAR tipo_anexo "COMPROVANTE, NOTA_FISCAL, RECIBO, CONTRATO, OUTRO"
        VARCHAR url
    }
    IMOVEL_FOTO {
        BIGSERIAL id PK
        BIGINT imovel_id FK
        VARCHAR url
    }
    IMOVEL_DOCUMENTO {
        BIGSERIAL id PK
        BIGINT imovel_id FK
        VARCHAR tipo_documento "MATRICULA, ESCRITURA, ALVARA, ART, HABITE_SE..."
        VARCHAR fase_imovel
        VARCHAR url
    }

    PESSOA ||--o| FORNECEDOR : "pode ser"
    PESSOA ||--o{ DESPESA : "paga"
    PESSOA ||--o{ DESPESA : "recebe"
    PESSOA ||--o{ IMOVEL : "vende/compra"
    PESSOA ||--o{ CONTRATO_FINANCEIRO : "é contraparte"
    IMOVEL ||--o{ DESPESA : "possui"
    IMOVEL ||--o{ IMOVEL_FOTO : "galeria"
    IMOVEL ||--o{ IMOVEL_DOCUMENTO : "documentos"
    IMOVEL ||--o{ CONTRATO_FINANCEIRO : "encadeia"
    CONTRATO_FINANCEIRO ||--o{ PARCELA_CONTRATO : "cronograma"
    CONTRATO_FINANCEIRO ||--o{ DESPESA : "custos acessórios"
    CATEGORIA_DESPESA ||--o{ DESPESA : "classifica"
    DESPESA ||--o{ DESPESA_ANEXO : "comprovantes e notas"
```

## Regras que não estão óbvias só olhando o schema

- `despesa.imovel_id` **nulo é intencional**: gasto geral (contador, combustível,
  ferramentas) que não entra no custo de nenhum imóvel (ADR-023)
- `despesa.fase_imovel` guarda a fase **em que a despesa foi incorrida**, não a
  fase atual do imóvel — é o que faz lançamento retroativo cair no lugar certo
- `imovel.fase` só avança; as três datas de transição são gravadas
  automaticamente e não podem ser reconstruídas depois (ADR-020)
- `contrato_financeiro.valor_quitacao` é o valor **negociado** da quitação
  antecipada, independente da soma das parcelas em aberto — e quitar não altera
  os valores originais das parcelas (ADR-025)
- A soma das parcelas **pode exceder** `valor_contratado` legitimamente (juros);
  não existe validação disso
- FKs para `pessoa` em relações financeiras: `ON DELETE RESTRICT`, e soft delete
  (`ativo`) em `imovel`, `pessoa`, `fornecedor` e `despesa` (ADR-028)
- Regras de negócio que não são constraint de banco (regra de custo, transições
  de fase) estão em `.agents/rules/`, que carregam sozinhas no escopo delas
