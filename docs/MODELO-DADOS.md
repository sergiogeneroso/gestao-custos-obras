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
        VARCHAR endereco "logradouro"
        VARCHAR numero
        VARCHAR bairro
        VARCHAR cidade
        VARCHAR uf
        VARCHAR cep
        VARCHAR observacao_endereco "complemento, referência"
        VARCHAR lote_matricula "DadosLote"
        VARCHAR lote_cartorio
        DATE lote_data_registro
        VARCHAR lote_inscricao_municipal
        NUMERIC area_lote "10,2"
        NUMERIC area_construida "10,2 — DadosConstrucao"
        DATE data_inicio_construcao
        NUMERIC custo_estimado_obra "14,2"
        DATE previsao_conclusao
        VARCHAR construcao_alvara_numero
        DATE construcao_alvara_emissao
        DATE construcao_alvara_validade
        VARCHAR construcao_art_numero
        BIGINT construcao_responsavel_tecnico_id FK
        VARCHAR construcao_cno
        DATE data_conclusao_obra "DadosCasa"
        VARCHAR casa_habite_se_numero
        DATE casa_habite_se_data
        DATE casa_data_averbacao
        INT casa_quartos
        INT casa_suites
        INT casa_banheiros
        INT casa_vagas_garagem
        NUMERIC compra_valor "14,2"
        DATE compra_data "marco inicial do ciclo (ADR-032)"
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
- `imovel.fase` só avança; as datas de transição são **informadas pelo usuário**
  na ação de transição — nunca gravadas automaticamente — e não podem ser
  reconstruídas depois (ADR-020)
- `imovel.compra_data` é o **marco inicial** da carteira e da fase LOTE; não
  existe `data_inicio_lote` separada (ADR-032)
- As colunas com prefixo `lote_`, `construcao_` e `casa_` são os `@Embedded`
  `DadosLote`/`DadosConstrucao`/`DadosCasa` (ADR-031) — agrupamento lógico numa
  tabela só, sem tabela por fase. Nulos nas fases ainda não atingidas são
  esperados, e nenhum desses campos é obrigatório na transição
- O endereço fica todo na raiz, no formato convencional (logradouro, número,
  bairro, cidade, UF, CEP e uma observação livre), porque vale para o imóvel
  inteiro e não muda quando a fase avança
- `contrato_financeiro.valor_quitacao` é o valor **negociado** da quitação
  antecipada, independente da soma das parcelas em aberto — e quitar não altera
  os valores originais das parcelas (ADR-025)
- A soma das parcelas **pode exceder** `valor_contratado` legitimamente (juros);
  não existe validação disso
- FKs para `pessoa` em relações financeiras: `ON DELETE RESTRICT`, e soft delete
  (`ativo`) em `imovel`, `pessoa`, `fornecedor` e `despesa` (ADR-028)
- Regras de negócio que não são constraint de banco (regra de custo, transições
  de fase) estão em `.agents/rules/`, que carregam sozinhas no escopo delas
