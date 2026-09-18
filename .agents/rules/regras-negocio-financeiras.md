---
paths:
  - "backend/src/main/java/**/despesa/**"
  - "backend/src/main/java/**/pessoa/**"
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

## Exclusão lógica: nunca DELETE físico em entidade com valor histórico (ADR-040)

Convenção geral do projeto, não só destas seis: `Imovel`, `Pessoa`, `Despesa`,
`ContratoFinanceiro`, `ParcelaContrato` e `OrcamentoCategoria` usam o
`@Embeddable` compartilhado `shared/exclusao/ExclusaoLogica.java` (`ativo`,
`motivoExclusao` obrigatório, `excluidoEm`, `excluidoPor`) em vez de um
`Boolean ativo` solto. Todo domínio novo nasce assim por padrão (ver skill
`gerar-crud-dominio`) — exceção só para catálogo global sem histórico (ex.
`CategoriaDespesa`, que continua delete físico).

- Usar `findByAtivoTrue()` / `findByIdAndAtivoTrue()` — o nome do método é o
  mesmo em toda entidade, ainda que por baixo seja `@Query` contra
  `x.exclusao.ativo = true` em vez de query-method derivado
- `Imovel` com `exclusao.ativo = false` não aparece em listagens nem aceita
  novas despesas
- `Pessoa` com `exclusao.ativo = false` não pode ser vinculada a novas
  despesas como pagadora ou beneficiária
- **Excluir o imóvel cascateia**: despesas, contratos financeiros (e suas
  parcelas/documentos), orçamento por categoria ficam `ativo = false`; fotos,
  documentos do imóvel e documentos de contrato são removidos de verdade
  (arquivo físico incluso) — eles não carregam resultado financeiro a
  proteger. Sempre permitido, em qualquer fase/situação, mesmo com contrato
  quitado ou parcela já paga: a cascata do imóvel não tem a trava que a
  exclusão avulsa de contrato tem (ver `contratos-financeiros.md`)
- **Motivo é obrigatório em toda exclusão lógica**, inclusive as retroativas
  (Imóvel, Pessoa, Despesa) — o service recusa gravar sem ele
- Reverter `situacao` de um imóvel vendido **não é mais fora de escopo**
  (ADR-043) — ver `ciclo-vida-imovel.md`; a cascata que isso dispara no
  `PARCELAMENTO_VENDA` usa exclusão lógica normalmente. Ainda fora de
  escopo: endpoint de restauração de exclusão lógica (ver
  `docs/PROXIMOS-PASSOS.md`)

## Despesa (ADR-023)

- `pagador` é **obrigatório**; `beneficiario` é **opcional** (nem sempre se sabe
  quem recebeu no momento do lançamento no canteiro)
- `beneficiario` referencia **Pessoa** — cobre quem recebeu sem estar marcado
  como fornecedor (vendedor do lote, banco, diarista). Fornecedor é só uma marca
  no cadastro de Pessoa (ADR-034), não um domínio à parte
- `imovel` é **opcional**: sem imóvel = gasto geral (contador, combustível,
  ferramentas), que fica fora do custo de qualquer imóvel
- Valor sempre **positivo**. Devolução de material se resolve editando ou
  inativando o lançamento original, nunca com valor negativo
- Não existe rateio entre pessoas nem entre imóveis: um custo dividido são dois
  lançamentos
- **Despesa sem anexo do tipo `COMPROVANTE` é destacada na listagem** (aviso
  visual, nunca bloqueio — o comprovante às vezes chega depois do lançamento).
  `RECIBO`/`NOTA_FISCAL`/outros tipos não contam: só `COMPROVANTE` é a prova de
  pagamento (Set 2026). Calculado em `DespesaService.buscar`, campo
  `temComprovante` no `DespesaResponseDTO` — nulo fora da busca paginada
  significa "não calculado", nunca "sem comprovante"

## Indicadores de apresentação (ADR-039)

Alguns números do relatório existem só para responder uma pergunta do usuário e
**nunca podem ser somados ao custo**. Já são dois:

- `despesasPorEtapa` — agrega por trecho da obra o mesmo dinheiro que já entra
  por fase (ADR-035)
- `custoSemCompra` — `totalDespesas + jurosPagos`: quanto o imóvel consumiu
  **depois de adquirido**, em qualquer fase. Ficam de fora `valorCompra` **e**
  `ajusteQuitacao`, porque os dois são preço do lote. Na carteira o agregado
  equivalente é `totalGastoSemCompras`

Nenhum dos dois participa de `custoTotal`, `lucro`, `margem`,
`rentabilidadeAnualizada`, `totalInvestido` ou `lucroRealizado` — incluí-los
contaria as mesmas despesas duas vezes e faria o resultado mentir.

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
