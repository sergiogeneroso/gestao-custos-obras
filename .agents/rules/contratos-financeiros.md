---
paths:
  - "backend/src/main/java/**/contratoFinanceiro/**"
  - "backend/src/main/java/**/relatorio/**"
---

# Contratos Financeiros (ADR-025)

## Contrato é opcional — tudo nesta rule só vale se existir um

Financiamento e parcelamento são opcionais em **cada** etapa da vida do imóvel: a
compra pode ser à vista ou parcelada, a obra pode ser feita com recurso próprio
ou financiada, e a venda pode ser à vista ou com entrada mais parcelas.

Um imóvel comprado à vista, construído com recurso próprio e vendido à vista
**não tem contrato nenhum** — e esse é um caso normal, não uma exceção. Para ele:
custo = valor de compra + despesas, sem juros a somar, sem saldo devedor e sem
parcelas a vencer. O relatório mostra só o bloco de custo.

Quando há contrato, um mesmo imóvel pode **encadear vários ao longo da vida**.
Um cenário real e completo: lote comprado parcelado direto com o vendedor →
quitação antecipada desse parcelamento (o banco exige o terreno livre para dar em
garantia) → financiamento de construção do banco → venda com o financiamento
quitado à vista na operação. Tipos: `PARCELAMENTO_COMPRA`,
`FINANCIAMENTO_CONSTRUCAO`, `PARCELAMENTO_VENDA`.

## Custo e caixa são coisas diferentes

Esta é a distinção que o modelo inteiro protege, e onde é fácil errar:

- **Custo** é o que o imóvel consumiu de recursos — a obra custou o que custou,
  tenha o dinheiro vindo do banco ou do bolso.
- **Caixa** é quando o dinheiro entra e sai — financiar só muda *quando* você
  paga, não *quanto* o imóvel custou.

Por isso:

- **Prestação de contrato nunca é lançada como despesa.** O dinheiro que o
  financiamento liberou já pagou despesas que foram lançadas (material, mão de
  obra); devolvê-lo ao banco não é custo novo, é devolver o que foi emprestado.
  Lançar os dois dobraria o custo do imóvel.
- **Saldo devedor não é custo** — é posição de caixa. Exibir separado, nunca
  somado ao `custoTotal`.
- **Juros entram no custo**, via `valorJuros` das parcelas efetivamente pagas —
  só essa parte, nunca a parcela inteira. Juros e tarifas são o custo real de
  usar o dinheiro do banco.
- **Na venda parcelada vale o espelho:** a receita é o valor da venda, registrado
  na venda; receber cada parcela é caixa entrando, não receita nova.
- **`PARCELAMENTO_VENDA` é crédito, não dívida — o tipo do contrato decide de que
  lado ele conta.** Os dois outros tipos são dinheiro que você deve; este é
  dinheiro que o comprador te deve. Portanto: o `valorJuros` das parcelas
  recebidas **nunca** entra em `jurosPagos`/`custoTotal` (é juro que entrou no
  caixa, e somá-lo ao custo derruba o lucro a cada parcela que o comprador paga),
  e as parcelas em aberto são **a receber**, nunca saldo devedor nem parcela a
  vencer. Em `RelatorioService` isso vive num único ponto de decisão, `ehDivida`.

Exemplo: lote 100k + obra 200k lançada como despesa + 3k de vistorias e tarifas,
com financiamento de 200k quitado por 205k na venda de 380k. Custo = 100 + 200 +
3 + 5 (juros) = **308k**, lucro **72k**. Somar a quitação ao custo daria 513k e
um prejuízo inexistente de 133k.

## Regras do contrato

- **A quitação antecipada tem valor próprio, negociado**, independente da soma
  das parcelas em aberto (normalmente menor, com desconto). Registrar
  `dataQuitacao` e `valorQuitacao` no contrato e encerrar as parcelas em aberto
  **sem alterar os valores originais delas** — o histórico do que foi contratado
  precisa continuar legível.
- **Não validar a soma das parcelas contra o `valorContratado`.** Juros fazem a
  soma exceder o principal legitimamente; essa validação quebraria em uso normal.
- Pagar uma prestação é **dar baixa na parcela**, nunca criar uma despesa.

## Custos acessórios do financiamento

Vistoria de engenharia a cada medição, avaliação, tarifas, seguro e registro da
hipoteca **são despesas comuns** (não prestações), na categoria "Custos de
financiamento", com a instituição como beneficiária e a FK opcional
`contratoFinanceiro` preenchida. Quando a estratégia é quitar o financiamento à
vista logo na venda, são esses acessórios — e não os juros — que pesam no
resultado.

**Não registrar liberações do banco por medição:** não mudam nem o custo (que são
as despesas) nem a dívida (que é o contrato).

## Edição do contrato (ADR-036)

O contrato é editável por `PUT`, mas duas coisas são histórico fechado e o
service precisa recusar:

- **Contrato `QUITADO` não pode ser editado.** A quitação tem valor próprio,
  negociado, e as parcelas originais precisam continuar legíveis.
- **Parcela já paga (`dataPagamento != null`) não pode ser alterada nem
  removida** — número, vencimento, valor e `valorJuros`. O `valorJuros` dela já
  entrou em `jurosPagos` e no `custoTotal` do relatório; mudá-lo reescreveria um
  resultado já apurado. Por isso a comparação de "parcela inalterada" inclui
  `valorJuros`, não só valor e vencimento.

A edição governa somente as parcelas **em aberto**. Cuidado de implementação que
não pode ser perdido: a coleção `parcelas` usa `orphanRemoval = true`, então
`clear()` seguido de re-add **apaga do banco** as parcelas pagas antes de
reinseri-las. Remover apenas as não pagas e nunca recriar as pagas.
