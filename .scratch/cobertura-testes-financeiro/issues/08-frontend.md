# 08 — Frontend: cálculo do cronograma
Type: task
Status: open

Decisão 9. Extrair de
`frontend/src/app/features/contratos/contrato-form-dialog/contrato-form-dialog.ts`
para `features/contratos/cronograma.ts` funções puras: total do cronograma
(entrada + parcelas), diferença de juros (total − preço à vista), rateio de
juros proporcional ao valor com a sobra do arredondamento na última,
geração de N parcelas mensais preservando as pagas (dia 31 → último dia do
mês; numeração continua do maior número). **Aritmética em centavos
inteiros** (converter na entrada, devolver reais). O componente passa a
chamar as funções; comportamento visível igual.

`cronograma.spec.ts` com casos: sem juros fecha exato; rateio sem centavo
perdido (ex. 100,00 em 3); dia 31 em fevereiro; preservação de pagas e
numeração.

Também: extrair e testar `saldoAEstornar` do `contrato-detalhe-dialog.ts`
(soma do pago − estornado). Apagar `frontend/src/app/app.spec.ts`
(decisão 2). Rodar `cd frontend && npx ng test --watch=false` e
`npx ng build`.
