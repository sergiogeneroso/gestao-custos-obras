---
paths:
  - "backend/src/main/java/**/imovel/**"
  - "backend/src/main/java/**/despesa/**"
  - "frontend/src/app/features/imoveis/**"
---

# Ciclo de Vida do Imóvel (ADR-020)

O imóvel tem **dois eixos independentes**, e confundi-los é o erro mais fácil de
cometer aqui:

- **`fase`** (`FaseImovel`): `LOTE → CONSTRUCAO → CASA` — a natureza física
- **`situacao`** (`SituacaoImovel`): `ADQUIRIDO ⇄ A_VENDA → VENDIDO` — a
  situação comercial

## Regras que não podem ser quebradas

- **A fase só avança, nunca retrocede.** Todo imóvel começa como `LOTE`: neste
  negócio não se compra imóvel pronto para reformar
- **A venda pode ocorrer em qualquer fase** — inclusive na planta
  (`LOTE` + `VENDIDO`) ou com a obra em andamento (`CONSTRUCAO` + `VENDIDO`)
- **Vender não congela a fase.** A construção continua avançando depois da venda,
  e novas despesas de obra ainda chegam
- **Imóvel vendido continua aceitando despesa** — obra em andamento, corretagem,
  imposto sobre o ganho e acertos chegam depois da venda e são custo real
- **Resultado de imóvel vendido com obra pendente é provisório**
  (`situacao = VENDIDO` e `fase != CASA`): marcar como tal e mostrar apenas o
  realizado, sem projetar lucro
- **A despesa guarda a fase em que foi incorrida**, não a fase atual do imóvel —
  é o que faz lançamento retroativo cair no lugar certo. Preencher com a fase
  atual só como padrão, sempre editável
- **Fase e situação não mudam pelo PUT de cadastro.** Só por
  `PATCH /api/imoveis/{id}/fase` e `PATCH /api/imoveis/{id}/situacao`, porque
  essas ações gravam as datas de transição

## Cada fase tem propriedades próprias, pedidas no fluxo dela (ADR-031, ADR-033)

O imóvel guarda, além do financeiro, os dados documentais de cada fase, agrupados
em `@Embedded`: `DadosLote` (matrícula, cartório, data do registro, inscrição
municipal, área do lote), `DadosConstrucao` (área
construída, início, previsão, custo estimado, alvará, ART e responsável técnico,
CNO) e `DadosCasa` (conclusão da obra, habite-se, averbação, quartos/suítes/
banheiros/vagas).

- **O cadastro só pede o que é do lote.** Todo imóvel nasce `LOTE`: campos de
  construção, de casa e de venda não aparecem na criação
- **Cada propriedade é pedida no fluxo a que pertence** — os dados da construção
  no `PATCH /fase` para `CONSTRUCAO`, os da casa no `PATCH /fase` para `CASA`, e
  o valor de venda pretendido no `PATCH /situacao` para `A_VENDA`
- **Nenhum desses campos é obrigatório na transição.** Alvará e CNO saem depois
  do início da obra, e a metragem final às vezes só fecha no habite-se
- **Todos continuam editáveis pelo `PUT`**, que expõe as fases já alcançadas —
  corrigir dado lançado errado não pode depender de refazer a transição

## Datas de transição

`compra.data`, `dataInicioConstrucao` e `dataConclusaoObra` marcam **quando o
fato aconteceu**, não quando foi lançado no sistema. **Não existe
`dataInicioLote`**: a data da compra é o marco inicial da carteira e da fase
LOTE, e é obrigatória (ADR-032).

- **A data é sempre informada pelo usuário** na ação de transição. A sugestão
  preenchida no formulário — data de hoje nas transições, data da compra no
  cadastro do lote — é só um ponto de partida, nunca um valor gravado
  automaticamente: lançamento retroativo é comum, e a obra pode ter terminado
  semanas antes de alguém registrar isso
- **São editáveis depois.** Data de transição gravada errada precisa poder ser
  corrigida, porque alimenta os relatórios
- **Obrigatórias:** nenhuma transição de fase acontece sem a data correspondente.
  Não ser automática não significa poder ficar vazia
- **Ordem coerente:** `compra.data` ≤ `dataInicioConstrucao` ≤
  `dataConclusaoObra`. Validar na transição e na edição — data fora de ordem
  produziria tempo negativo por fase no relatório

**Não são decorativas:** alimentam tempo por fase, giro e rentabilidade, e são
dado que não pode ser reconstruído depois.

## Validações de transição

- `situacao = VENDIDO` exige valor e data de venda
- Ao iniciar a construção, **avisar** (sem bloquear) se ainda houver contrato de
  `PARCELAMENTO_COMPRA` com situação `ATIVO` — o banco costuma exigir o terreno
  quitado para financiar a obra
