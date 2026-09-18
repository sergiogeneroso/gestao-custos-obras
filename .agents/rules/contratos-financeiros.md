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

- **O contrato precisa de ao menos uma parcela ou uma entrada** — validação de
  classe em `ContratoFinanceiroRequestDTO.isCronogramaValido` (Set 2026). Um
  contrato quitado inteiro na entrada, sem nenhuma parcela futura, é válido
  (a entrada vira a parcela nº 0, à parte da lista `parcelas`).
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

## Exclusão do contrato (ADR-040)

`ContratoFinanceiroService.excluir` existe para corrigir cadastro errado —
não para desfazer um contrato que já rodou de verdade. A trava espelha a de
edição:

- **Recusa contrato `QUITADO`** ou **com qualquer parcela paga**
  (`dataPagamento != null`) — igual à edição, porque esse histórico já pode
  ter entrado em `jurosPagos`/`custoTotal` de um relatório apurado
- Passando na trava, cascateia: parcelas ficam `ativo = false`, documentos do
  contrato são removidos de verdade (registro + arquivo)
- **Despesa de custo acessório do financiamento é desvinculada, nunca
  excluída** (`contratoFinanceiro = null`) — é gasto real, independente do
  contrato estar certo ou errado
- **`PARCELAMENTO_COMPRA` único**: se `aplicarValorDoLote` gravou
  `imovel.compra.valor` a partir deste contrato e não sobra nenhum outro
  `PARCELAMENTO_COMPRA` (ativo ou quitado) no imóvel, o valor é limpo de
  volta para vazio — evita conservar um preço que veio do contrato que
  acabou de ser excluído
- **A cascata de exclusão do imóvel inteiro não passa por essa trava** —
  `ImovelExclusaoService` chama `ContratoFinanceiroService.cascatearExclusao`
  diretamente, sem checar `QUITADO`/parcela paga, porque excluir o imóvel é
  sempre permitido (ver `regras-negocio-financeiras.md`)

## Compra parcelada do lote (ADR-037) — só `PARCELAMENTO_COMPRA`

**O parcelamento do lote é normalmente SEM juros.** O padrão do negócio é entrada
mais parcelas em que o restante do preço é apenas dividido; juros existem, mas são
minoritários e dependem do valor da entrada. Nenhum fluxo pode exigir informação
sobre juros para se completar — quem assume juros como regra está desenhando para
a exceção.

- **A entrada é a parcela nº 0**, criada já com `dataPagamento` e `valorPago`: é
  fato consumado na compra, não evento futuro. Não inventar campo próprio no
  contrato — como parcela ela já entra em total pago e saldo devedor, e na edição
  cai sob a guarda de parcela paga.
- **Na compra parcelada, `compraValor` é o preço do lote**, gravado por
  `ContratoFinanceiroService.criar`: o `precoAVistaLote` informado ou, na falta,
  `entrada + Σ parcelas`. **Nunca sobrescrever** valor já preenchido e **nunca**
  regravar ao editar o cronograma — isso mudaria em silêncio o custo de um imóvel
  já apurado.
- **`valorJuros` fica nulo** sempre que entrada + parcelas fecharem com o preço,
  que é o esperado.

### Ajuste de quitação: comparar com o PRINCIPAL, não com o total

Ponto onde é fácil errar. A fórmula ingênua `valorQuitacao − Σ parcelas em aberto`
funciona sem juros e erra feio com juros, porque as parcelas em aberto carregam
juros que nunca foram pagos e portanto nunca entraram no custo — subtraí-los de
novo derruba o custo indevidamente.

```
principalEmAberto = Σ (parcela.valor − coalesce(parcela.valorJuros, 0))
                    das parcelas sem dataPagamento
ajusteQuitacao    = valorQuitacao − principalEmAberto
```

Negativo é desconto e abate o custo; positivo é a parte de juros embutida no valor
negociado e soma. **A invariante que prova a fórmula:** num `PARCELAMENTO_COMPRA`
quitado, `custoTotal` do lote converge exatamente para o desembolso real. As
parcelas originais continuam intocadas — o ajuste é calculado, nunca gravado.

## Venda desfeita: contrato cancelado e estorno (ADR-043) — só `PARCELAMENTO_VENDA`

Quando o imóvel sai de `VENDIDO` (ver `ciclo-vida-imovel.md`), o
`PARCELAMENTO_VENDA` vinculado (se houver) é resolvido automaticamente na
mesma operação, sem passo manual do usuário e sem nunca bloquear o
`PATCH /situacao` do imóvel:

- **Sem parcela paga:** o contrato é excluído.
- **Com ao menos uma parcela paga:** o contrato vira
  `SituacaoContrato.CANCELADO` — deliberadamente diferente de `QUITADO`
  (que significa "cumprido com sucesso"). Sem essa cascata, o contrato
  ficaria `ATIVO` contando em `saldoAReceberTotal` como dinheiro que ainda
  vai entrar, de um negócio que já caiu.

Um contrato `CANCELADO` guarda `dataCancelamento` e `motivoCancelamento`
próprios — não depende do log de auditoria do Imóvel para se explicar. A
cascata em si gera um evento de auditoria próprio no `ContratoFinanceiro`
(não é silenciosa como a cascata de exclusão do imóvel inteiro).

**Estorno** é o dinheiro já recebido do comprador que precisa voltar,
possivelmente aos poucos e sem prazo definido:

- `valorEstornado` no contrato começa em zero e só cresce, via
  `registrarEstorno(contratoId, data, valor)` — cada baixa parcial soma
  nele. **Nunca existe cronograma de parcelas de estorno** — seria
  duplicar a máquina de `ParcelaContratoModel` para um caso que não tem
  parcelamento negociado, só devoluções avulsas.
- "Quanto falta devolver" é sempre **calculado**
  (`Σ parcelas pagas − valorEstornado`), nunca gravado — mesmo espírito de
  nunca alterar os valores originais das parcelas.
- `saldoAEstornarTotal`, consolidado na Carteira, soma isso entre todos os
  contratos `CANCELADO` — mesmo padrão de `saldoDevedorTotal`/
  `saldoAReceberTotal`. **Fora de `custoTotal`/`lucro`** em qualquer caso;
  a regra de custo não muda.

### Desembolso é caixa, não custo

`totalDesembolsado` e `saldoAPagar` do `ResultadoImovelDTO` são posição de caixa e
**nunca** somam ao `custoTotal` — mesma separação que já vale para saldo devedor.
`totalDesembolsado` só inclui o valor de compra quando `compra.parcelada` é falso;
na compra parcelada esse dinheiro flui pelas parcelas e contá-lo duas vezes
dobraria o desembolso.
