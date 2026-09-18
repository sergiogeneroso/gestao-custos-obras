---
paths:
  - "backend/src/test/**/relatorio/**"
  - "backend/src/test/**/contratoFinanceiro/**"
  - "backend/src/test/**/imovel/**"
  - "backend/src/test/**/despesa/**"
  - "backend/src/test/**/orcamentoCategoria/**"
---

# Testes do domínio financeiro e do ciclo de vida

Estes são os testes que impedem o resultado do imóvel de mentir. As regras em
si estão em `regras-negocio-financeiras.md`, `contratos-financeiros.md`,
`ciclo-vida-imovel.md` e `auditoria.md` (leia a correspondente antes de
escrever o teste); aqui está **quais cenários** precisam existir.

## Critério de cobertura

Cada bullet dessas quatro rules tem ao menos um teste nomeado pela regra, ou
está na lista de "estáticas" abaixo. Mudar uma regra atualiza ou cria o
teste correspondente antes de considerar a mudança pronta — regra nova sem
teste correspondente é trabalho inacabado, não uma lacuna para depois.

Orçamento por categoria (`orcamentoCategoria/`, `orcadoVsRealizado`) é
módulo pós-MVP (ver `docs/PROXIMOS-PASSOS.md`): os testes que já existem
ficam, mas ele fica fora do critério — ausência de teste ali não é lacuna.

## Cenários cobertos, por arquivo

**`RelatorioServiceTest`** — custo vs. caixa (`jurosDaParcelaEntramNoCustoMasAPrestacaoInteiraNao`,
`saldoDevedorNaoEntraNoCusto`); sem contrato = compra + despesas
(`semContratoCustoEApenasCompraMaisDespesas`); gasto geral fora do custo
(`gastoGeralNaoEntraNoCustoDoImovel`); indicadores de apresentação nunca
somam ao custo (`custoSemCompraSomaDespesas...`,
`despesasPorEtapaNuncaEntraNoCusto...`); compra parcelada sem/com juros,
quitação com desconto ou juros embutidos (`loteParceladoSemJuros...`,
`descontoNaQuitacaoDoLote...`, `quitacaoComJurosEmbutidos...`);
`PARCELAMENTO_COMPRA` quitado antecipadamente encadeado com
`FINANCIAMENTO_CONSTRUCAO` ativo no mesmo imóvel
(`parcelamentoCompraQuitadoMaisFinanciamentoConstrucaoAtivoSomamNoMesmoResultado`);
`PARCELAMENTO_VENDA` é a receber, não dívida
(`parcelamentoDeVendaContaComoAReceberNaoComoDivida`,
`jurosDeParcelamentoDeVendaNaoEntramNoCusto`); venda desfeita com parcela
paga vira saldo a estornar (`vendaDesfeitaComParcelaPagaContaComoSaldoAEstornar...`);
vendido com obra pendente é provisório, lote sem obra não é
(`resultadoProvisorioQuandoVendidoComObraPendente`,
`loteRevendidoSemObraNaoTemResultadoProvisorio`); custo por m² usa área
construída (`custoObraPorM2...`).

**`RelatorioCarteiraServiceTest`** — orçado vs. realizado, extratos por
pessoa, indicadores da carteira, saldo a estornar fora do lucro realizado
(`contratoCanceladoNaoEntraEmTotalInvestidoNemLucroRealizadoMasEntraEmSaldoAEstornar`).

**`ContratoFinanceiroServiceTest`** — entrada como parcela nº 0
(`entradaViraParcelaZeroJaBaixadaNaDataInformada`); valor do lote deduzido
do cronograma e nunca sobrescrito/regravado
(`valorDoLoteJaPreenchidoNuncaEhSobrescrito`,
`atualizarNuncaRegravaValorDeCompraAoEditarCronograma`); `pagarParcela`
recusa os três casos do ADR-044 (`pagarParcelaRecusaParcelaJaPaga`,
`pagarParcelaRecusaContratoQuitado`, `pagarParcelaRecusaContratoCancelado`);
edição recusa contrato quitado e parcela paga
(`atualizarRecusaContratoQuitado`,
`atualizarRecusaAlterarOuRemoverParcelaPagaInclusiveValorJuros`); exclusão
recusa quitado/parcela paga e cascateia
(`excluirRecusaContratoQuitado`,
`excluirContratoAtivoMarcaParcelasInativasEZeraValorDoLoteQuandoUnico`,
`excluirDesvinculaDespesaDeCustoAcessorioSemExcluiLa`); cancelamento e
estorno por venda desfeita (`cancelarPorVendaDesfeitaGravaSituacaoDataMotivoEZeraEstorno`,
`registrarEstornoAcumulaOValorDevolvidoEGravaAData`); auditoria de cada
mutação com estado anterior capturado antes de mutar
(`criarAuditaCriacaoComEstadoAnteriorNulo`,
`pagarParcelaAuditaComEstadoAnteriorAntesDaBaixa`,
`cascatearExclusaoNaoGeraEventoDeAuditoriaProprio`).

**`DespesaServiceTest`/`OrcamentoCategoriaServiceTest`** — etapa de obra só
em fase Construção (`etapaDeObraEmDespesaDeLoteEhRecusada`,
`etapaDeObraEmDespesaDeConstrucaoEhAceita`); fase informada vence a fase
atual (`faseInformadaVenceAFaseAtualDoImovel`); pessoa/imóvel/contrato
inativos são recusados (`pagadorInativoNaoPodeSerVinculado`,
`imovelInativoNaoAceitaDespesa`, `contratoExcluidoEhTratadoComoNaoEncontrado`
— bug corrigido); imóvel vendido continua aceitando despesa
(`imovelVendidoAceitaDespesa`); `temComprovante` só conta anexo COMPROVANTE
(`buscaMarcaTemComprovanteSoParaDespesaComAnexoDoTipoComprovante`);
duplicidade de orçamento ignora registro excluído — bug corrigido
(`orcamentoExcluidoNaoBloqueiaCriarOutroParaMesmaCategoria`); despesa de
custo acessório vinculada a contrato válido grava o vínculo sem alterar o
contrato (`despesaDeCustoAcessorioVinculadaAContratoAtivoGravaOVinculoSemAlterarOContrato`);
auditoria de criar/atualizar/excluir nos dois domínios.

**`ImovelServiceTest`/`ImovelExclusaoServiceTest`** — `criar` audita
`CRIACAO` com `estadoAnterior` nulo (`criarAuditaCriacaoComEstadoAnteriorNulo`);
fase só avança, nunca
retrocede nem pula (`faseNaoRetrocede`, `transicaoQuePulaFaseEhRecusada`);
venda em qualquer fase sem mudar fase, avanço de fase sem mudar situação —
eixos independentes (`venderGravaValorDataCompradorSemAlterarFase`,
`avancarFaseDeImovelVendidoNaoAlteraSituacao`); ordem coerente das datas
(`putComConclusaoDaObraAntesDoInicioEhRecusado`,
`transicaoComDataAnteriorAFaseAnteriorEhRecusada`); desfazer venda exige
motivo, limpa venda e cascateia o contrato
(`desfazerVendaSemMotivoEhRecusado`, `desfazerVendaLimpaCamposDeVenda`,
`desfazerVendaExcluiContratoDeVendaSemParcelaPaga`,
`desfazerVendaCancelaContratoDeVendaComParcelaPaga`); exclusão em cascata
ignora a trava de contrato quitado/parcela paga e gera um único evento de
auditoria (`excluirCascateiaContratoQuitadoDespesaOrcamentoFotosEDocumentos`).

**Repositórios, perfil `test` (ADR-045)** — filtros `AtivoTrue`/`@Query` JPQL
contra o banco `gestao_custos_obras_test`: `ImovelRepositoryTest`,
`DespesaRepositoryTest`, `ContratoFinanceiroRepositoryTest`,
`OrcamentoCategoriaRepositoryTest` (todos `findBy...AtivoTrue...`) e
`BuscasPaginadasTest` (busca de pessoa/contrato/categoria).

**Frontend** — `cronograma.spec.ts` cobre `totalCronograma`, `diferencaJuros`,
`distribuirJuros`, `gerarParcelas`, `somarMeses`, `proximoNumero` em centavos
inteiros; `contrato.model.spec.ts` cobre `saldoAEstornar`.

## Regras estáticas/não testáveis

- BigDecimal/`NUMERIC` obrigatórios para dinheiro (garantido em compilação e
  schema)
- `ExclusaoLogica` como padrão estrutural de entidade; Bean Validation
  simples (`@NotBlank`/`@NotNull` em endereço, vendedor, datas de transição)
  sem `isXxxValido()` próprio — framework já testado por quem o escreveu
- Ausência de funcionalidade: cronograma de parcelas de estorno, liberação
  do banco por medição lançada como movimento
- Fora de escopo declarado: login/logout na auditoria, restauração de
  exclusão lógica, `PessoaService`, `CategoriaDespesaService`, orçamento
  por categoria (pós-MVP)
- Decisão de design sem comportamento a testar: auditoria manual em vez de
  Envers/AOP (ADR-042)

## Como montar os cenários

- Um teste por cenário, nome afirmando a regra; valores redondos e
  distintos entre si para a soma deixar óbvio o que entrou e o que ficou de
  fora
- Reaproveite os construtores `imovel(...)`, `contrato(...)`, `parcela(...)`,
  `despesa(...)` já existentes na classe em vez de criar novos
- Mudança em `RelatorioService` ou nas transições de `ImovelService` passa
  por plan mode (`CLAUDE.md`), e o plano lista os cenários acima que toca
