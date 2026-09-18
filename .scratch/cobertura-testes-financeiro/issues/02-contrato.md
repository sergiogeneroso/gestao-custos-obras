# 02 — Contrato financeiro
Type: task
Status: resolved

Em `ContratoFinanceiroServiceTest` (ampliar, reaproveitar construtores).

Trava nova (decisão 6), teste antes → vermelho → código:
- `pagarParcela` recusa parcela já paga, contrato QUITADO, contrato CANCELADO
- Registrar a regra em `.agents/rules/contratos-financeiros.md` e ADR-044 em
  `docs/DECISOES.md`

Sem teste hoje (comportamento existente, só cobrir):
- `atualizar`: recusa contrato QUITADO; recusa alterar/remover parcela paga
  (inclusive `valorJuros`); preserva as pagas (sem `clear()`); nunca regrava
  `compra.valor` ao editar o cronograma
- `quitar`: grava data/valor da quitação sem alterar parcelas; recusa já
  quitado
- `pagarParcela`: baixa grava data/valor; recusa parcela de outro contrato
- não valida soma das parcelas × `valorContratado` (aceita divergência)
- `excluir`/cascata: remove documentos do contrato; com outro
  PARCELAMENTO_COMPRA restante, mantém `compra.valor`
- `deletarDocumento`: recusa documento de outro contrato
