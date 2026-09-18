# 06 — Auditoria
Type: task
Status: open

Decisão 8. Ler `.agents/rules/auditoria.md` antes. Nos testes de Service de
contrato, despesa, imóvel e orçamento: um teste por método de mutação
(`criar`, `atualizar`, `excluir`, `avancarFase`, `alterarSituacao`,
`quitar`, `pagarParcela`, `cancelarPorVendaDesfeita`, `registrarEstorno`…)
verificando `auditoriaService.registrar(entidade, id, operação, anterior,
novo)`:
- entidade = nome da classe sem sufixo; CRIACAO com anterior nulo
- anterior e novo são ResponseDTO
- **anterior reflete o estado antes da mutação** (ArgumentCaptor, comparar
  um campo que muda)
- sub-recursos (fotos, documentos, anexos) não auditam
- exclusão do imóvel gera um único evento (auditoria em `excluir`, não em
  `cascatearExclusao`)
- cascata da venda desfeita gera auditoria própria do contrato
