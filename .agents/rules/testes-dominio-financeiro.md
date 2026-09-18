---
paths:
  - "backend/src/test/**/relatorio/**"
  - "backend/src/test/**/contratoFinanceiro/**"
  - "backend/src/test/**/imovel/**"
  - "backend/src/test/**/despesa/**"
---

# Testes do domínio financeiro e do ciclo de vida

Estes são os testes que impedem o resultado do imóvel de mentir. As regras em
si estão em `regras-negocio-financeiras.md`, `contratos-financeiros.md` e
`ciclo-vida-imovel.md` (leia a correspondente antes de escrever o teste); aqui
está **quais cenários** precisam existir e já existem.

## Cenários que toda mudança nesses pacotes mantém cobertos

Ao mexer em cálculo ou transição, confira que o cenário afetado tem teste; se
não tiver, ele é o primeiro a ser escrito.

**Custo e resultado** (`RelatorioServiceTest`)
- Imóvel **sem contrato nenhum** é caso normal, custo = compra + despesas
  (`semContratoCustoEApenasCompraMaisDespesas`)
- Juros pagos entram no custo; prestação inteira e saldo devedor não
  (`jurosDaParcelaEntramNoCustoMasAPrestacaoInteiraNao`,
  `saldoDevedorNaoEntraNoCusto`)
- Gasto geral fora do custo de qualquer imóvel
  (`gastoGeralNaoEntraNoCustoDoImovel`)
- Compra parcelada sem juros não muda de custo conforme paga; com juros só
  incorpora os pagos (`loteParcelado...`); desconto e juros na quitação
  (`descontoNaQuitacao...`, `quitacaoComJuros...`)
- Vendido com obra pendente = resultado provisório
  (`resultadoProvisorioQuandoVendidoComObraPendente`)
- Venda desfeita com parcela paga vira saldo a estornar
  (`vendaDesfeitaComParcelaPaga...`)

**Ciclo de vida** (`ImovelServiceTest`, `ImovelExclusaoServiceTest`)
- Fase não retrocede (`faseNaoRetrocede`); ordem das datas pelo PUT e pela
  transição (`putCom...`, `transicaoComDataAnterior...`)
- Desfazer venda: exige motivo, limpa venda, exclui ou cancela o contrato de
  venda conforme tenha parcela paga (`desfazerVenda...`)
- Exclusão em cascata e cálculo de impacto

**Contratos** (`ContratoFinanceiroServiceTest`)
- Entrada como parcela 0, valor do lote deduzido do cronograma, bloqueios de
  exclusão, cancelamento por venda desfeita e estorno

**Lacunas conhecidas** — ainda sem teste; escreva-o ao mexer no código
correspondente e mova-o para a lista acima:
- Transição que pula fase (`LOTE → CASA`) é recusada — só existe
  `faseNaoRetrocede`
- Vender na planta não altera a fase, e avançar a fase de um imóvel vendido
  não mexe na situação (independência dos dois eixos no `ImovelService`)
- Quitação antecipada do parcelamento do lote seguida de um financiamento de
  construção no mesmo imóvel, no `RelatorioService`

## Como montar os cenários

- Um teste por cenário da lista, com o nome afirmando a regra
  (`saldoDevedorNaoEntraNoCusto`)
- Valores redondos e distintos entre si (100000 de compra, 2000 de despesa,
  30 de juros) para que a soma esperada deixe óbvio o que entrou e o que
  ficou de fora
- Reaproveite os construtores `imovel(...)`, `contrato(...)`, `parcela(...)`,
  `cronograma(...)` já existentes na classe em vez de criar novos
- Mudança em `RelatorioService` ou nas transições de `ImovelService` passa
  por plan mode (ver `CLAUDE.md`), e o plano lista os cenários acima que ela
  toca
