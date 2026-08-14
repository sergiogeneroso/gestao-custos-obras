---
paths:
  - "backend/src/main/java/**/contratoFinanceiro/**"
  - "backend/src/main/java/**/relatorio/**"
---

# Contratos Financeiros (ADR-025)

Um mesmo imóvel **encadeia vários contratos ao longo da vida**, e não um único
cronograma de parcelas. O fluxo real do negócio é: lote comprado parcelado direto
com o vendedor → quitação antecipada desse parcelamento (o banco exige o terreno
livre para dar em garantia) → financiamento de construção do banco → venda, à
vista ou parcelada, com o financiamento quitado à vista na operação.

Tipos: `PARCELAMENTO_COMPRA`, `FINANCIAMENTO_CONSTRUCAO`, `PARCELAMENTO_VENDA`.

## Regras que não podem ser quebradas

- **Prestação de contrato nunca vira despesa.** As despesas que o financiamento
  custeou já são o custo; lançar as prestações também dobraria o custo do imóvel
- **Saldo devedor não é custo** — é posição de caixa. Exibir separado, nunca
  somado ao `custoTotal`
- **A quitação antecipada tem valor próprio, negociado**, independente da soma
  das parcelas em aberto (normalmente menor, com desconto). Registrar
  `dataQuitacao` e `valorQuitacao` no contrato e encerrar as parcelas em aberto
  **sem alterar os valores originais delas** — o histórico do que foi contratado
  precisa continuar legível
- **Não validar a soma das parcelas contra o `valorContratado`.** Juros fazem a
  soma exceder o principal legitimamente; essa validação quebraria em uso normal
- **Juros entram no custo do imóvel**, via `valorJuros` das parcelas efetivamente
  pagas — só essa parte, nunca a parcela inteira

## Custos acessórios do financiamento

Vistoria de engenharia a cada medição, avaliação, tarifas, seguro e registro da
hipoteca são **despesas comuns**, na categoria "Custos de financiamento", com a
instituição como beneficiária e a FK opcional `contratoFinanceiro` preenchida.
Como a estratégia do negócio é quitar o financiamento à vista logo na venda, são
esses acessórios — e não os juros — que costumam pesar no resultado.

**Não registrar liberações do banco por medição:** não mudam nem o custo (que são
as despesas) nem a dívida (que é o contrato).
