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
  juros efetivamente pagos nas parcelas dos contratos
- **Prestação de financiamento NÃO é despesa** — a obra já foi lançada como
  despesa; contar as duas dobraria o custo
- **Saldo devedor quitado na venda NÃO é custo** — é devolução do principal que
  pagou despesas já contadas. Exibir como posição de caixa, nunca somado ao custo
- **Gasto geral (despesa sem imóvel) não entra no custo de imóvel nenhum** e não
  é rateado entre eles

## Valores monetários

- **Sempre** `BigDecimal` (Java) + `NUMERIC(14,2)` (PostgreSQL). Proibido
  `double`/`float`/`Double` para dinheiro
- Única exceção: indicador **percentual** que exige expoente fracionário
  (rentabilidade anualizada usa `Math.pow`). Nunca para valor monetário, e
  sempre com comentário no ponto de uso

## Soft delete: nunca DELETE físico em entidade financeira

- `ativo BOOLEAN` em `Imovel`, `Pessoa`, `Fornecedor` e `Despesa` (ADR-028).
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
