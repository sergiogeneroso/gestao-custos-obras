# 03 — Relatório
Type: task
Status: open

Em `RelatorioServiceTest`. Só cobrir comportamento existente — se o código
contradisser uma rule, PARAR e relatar (decisão 11).

- `orcadoVsRealizado` (e RNE sem imovelId)
- `custoPorImovel` (filtros)
- `extratoPessoas` (total por pagador) e `historicoFornecedor` (por
  beneficiário)
- `custoPorM2`: RNE sem imovelId
- `resultadoImovel`: `rentabilidadeAnualizada`; `despesasPorEtapa` nunca
  entra no custo e despesa sem etapa fica fora do quadro (sem chave nula);
  tempo por fase com obra concluída (CONSTRUCAO/CASA)
- `carteira`: `totalVendido`, `lucroRealizado`, `totalGastoSemCompras`,
  `gastosGeraisPeriodo` (e seu filtro de data), `imoveisPorFase`,
  `imoveisPorSituacao`; `saldoAEstornarTotal` fora de custo e lucro
- Venda parcelada: parcela recebida é caixa, não receita (hoje só parcial)
