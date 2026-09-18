# 07 — Imóvel (lacunas do ciclo de vida)
Type: task
Status: open

Em `ImovelServiceTest`. Só cobrir existente; contradição com rule → parar.
- transição que pula fase (LOTE → CASA) é recusada
- vender (alterarSituacao VENDIDO) não altera fase; avançar fase de vendido
  não altera situação; vendido aceita despesa (se couber em
  `DespesaServiceTest`, pôr lá)
- VENDIDO grava valor/data/comprador; A_VENDA grava `valorPretendido`;
  desfazer venda preserva `valorPretendido`
- PUT não muda fase nem situação
- `validarOrdemDatas`: conclusão antes da compra sem início de obra
- aviso de PARCELAMENTO_COMPRA ativo ao avançar para CONSTRUCAO
- `deletarFoto`/`deletarDocumento` recusam item de outro imóvel
- `LocalStorageService`: recusa nome com `..` (decisão 10) — teste próprio
