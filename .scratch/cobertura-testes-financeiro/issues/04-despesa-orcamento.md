# 04 — Despesa e orçamento
Type: task
Status: open

Bugs (decisão 7), teste vermelho primeiro:
- `DespesaService.buscarContratoOpcional`: contrato excluído (inativo) →
  "não encontrado"
- Orçamento excluído não bloqueia criar outro para mesma categoria/imóvel

`DespesaServiceTest` — sem teste hoje:
- `atualizar` aplica as mesmas regras de fase/etapa de `criar`
- `buscar` marca `temComprovante` (e fica nulo fora da busca)
- `excluir` exclusão lógica com motivo
- `deletarAnexo` recusa anexo de outra despesa
- pessoa inativa não pode ser vinculada; imóvel inativo não aceita despesa;
  pagador obrigatório, beneficiário opcional; valor sempre positivo (se a
  trava for só Bean Validation no DTO, testar o DTO com `Validator`)
- motivo obrigatório em exclusão lógica (`ExclusaoRequestDTO` `@NotBlank`,
  via `Validator`)

`OrcamentoCategoriaServiceTest` (novo): duplicidade na criação/edição;
`totalGasto` = soma das despesas do imóvel na categoria; excluir lógico.
