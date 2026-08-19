---
paths:
  - "backend/src/main/java/**/despesa/**"
  - "backend/src/main/java/**/pessoa/**"
  - "backend/src/main/java/**/fornecedor/**"
  - "backend/src/main/java/**/imovel/**"
  - "backend/src/main/java/**/contratoFinanceiro/**"
  - "backend/src/main/java/**/relatorio/**"
---

# Regras de Negócio Financeiras (não quebrar)

## A regra de custo (ADR-025)

Errar qualquer um destes pontos faz o resultado do imóvel mentir:

- **Custo do imóvel** = valor de compra + despesas do imóvel (todas as fases,
  incluindo custos acessórios do financiamento e o imposto sobre o ganho) +
  juros efetivamente pagos nas parcelas, **quando houver contrato**
- **Gasto geral (despesa sem imóvel) não entra no custo de imóvel nenhum** e não
  é rateado entre eles

Contratos financeiros são **opcionais**: compra à vista, obra com recurso próprio
e venda à vista formam um imóvel sem contrato nenhum, que é caso normal. Para
ele, a regra encolhe para **custo = compra + despesas**, sem juros, sem saldo
devedor e sem bloco de caixa no relatório.

Havendo contrato, valem também (detalhe em `contratos-financeiros.md`):

- **Prestação NÃO é despesa** — o dinheiro liberado já pagou despesas lançadas;
  devolvê-lo ao banco não é custo novo. Pagar prestação é dar baixa na parcela
- **Saldo devedor NÃO é custo** — é posição de caixa, nunca somado ao custo
- **Na venda parcelada, receber parcela é caixa**, não receita nova: a receita é
  o valor da venda

## Valores monetários

- **Sempre** `BigDecimal` (Java) + `NUMERIC(14,2)` (PostgreSQL). Proibido
  `double`/`float`/`Double` para dinheiro
- Única exceção: indicador **percentual** que exige expoente fracionário
  (rentabilidade anualizada usa `Math.pow`). Nunca para valor monetário, e
  sempre com comentário no ponto de uso

## Soft delete: nunca DELETE físico em entidade financeira

- `ativo BOOLEAN` em `Imovel`, `Pessoa` e `Despesa` (ADR-028).
  Usar `findByAtivoTrue()` / `findByIdAndAtivoTrue()`
- `Imovel` com `ativo = false` não aparece em listagens nem aceita novas despesas
- `Pessoa` com `ativo = false` não pode ser vinculada a novas despesas como
  pagadora ou beneficiária

## Despesa (ADR-023)

- `pagador` é **obrigatório**; `beneficiario` é **opcional** (nem sempre se sabe
  quem recebeu no momento do lançamento no canteiro)
- `beneficiario` referencia **Pessoa**, não Fornecedor — cobre quem recebeu sem
  ter cadastro de fornecedor (vendedor do lote, banco, diarista)
- `imovel` é **opcional**: sem imóvel = gasto geral (contador, combustível,
  ferramentas), que fica fora do custo de qualquer imóvel
- Valor sempre **positivo**. Devolução de material se resolve editando ou
  inativando o lançamento original, nunca com valor negativo
- Não existe rateio entre pessoas nem entre imóveis: um custo dividido são dois
  lançamentos

## Catálogo

- `CategoriaDespesa` é catálogo global (não por imóvel) — cadastrada uma vez,
  reutilizada em qualquer imóvel. Responde só pela **natureza** do gasto; o eixo
  temporal é a fase do imóvel (ver `ciclo-vida-imovel.md`)

## Etapa da construção (ADR-035)

`despesa.etapaConstrucao` é um **enum fixo** e opcional, e só é aceito quando
`faseImovel == CONSTRUCAO` — `DespesaService` recusa a combinação contrária.

Ele responde "quanto custou cada trecho da obra" e **não concorre com
`CategoriaDespesa`**: categoria é a natureza do gasto, fase é o momento na vida
do imóvel, etapa é a parte da construção.

`despesasPorEtapa`, no relatório, é **apresentação sobre despesas já
contabilizadas**: agrega o mesmo dinheiro que já entra por fase e **não pode**
ser somado a `custoTotal` — fazê-lo dobraria o custo da obra. Despesa sem etapa
fica fora do quadro, nunca vira chave nula na agregação.
