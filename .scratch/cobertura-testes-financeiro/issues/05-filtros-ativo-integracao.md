# 05 — Filtros AtivoTrue com banco real
Type: task
Status: open
Blocked by: 01

Testes de repository (perfil `test`, rollback por teste) que provam que
registro inativo não volta, nos métodos que alimentam custo:
- `DespesaRepository`: `findByImovelIdAndAtivoTrue`, `findByAtivoTrue`,
  `findByImovelIsNullAndAtivoTrue` (e que só traz despesa sem imóvel),
  `findByImovelIdAndCategoriaDespesaIdAndAtivoTrue`, `buscar` (escopos
  TODAS/IMOVEL/GERAL)
- `ContratoFinanceiroRepository.findByImovelId` (só ativos)
- `ImovelRepository.findByAtivoTrue`, `buscar` (filtro fase/situação)
- `OrcamentoCategoriaRepository.findByImovelId`
- `DespesaAnexoRepository.listarDespesaIdPorAnexoDoTipo` (só COMPROVANTE)

Poucos testes, cada um com um registro ativo e um inativo lado a lado.
Substituir os `isNotNull()` de `BuscasPaginadasTest` por asserção real
(decisão 2) ou fundir nesta etapa.
