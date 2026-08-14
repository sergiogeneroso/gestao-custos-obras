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

## Datas de transição

`dataInicioLote`, `dataInicioConstrucao` e `dataConclusaoObra` são preenchidas
automaticamente quando a fase avança. **Não são decorativas:** alimentam tempo
por fase, giro e rentabilidade, e são dado que não pode ser reconstruído depois.
Nunca deixar uma transição de fase acontecer sem gravar a data correspondente.

## Validações de transição

- `situacao = VENDIDO` exige valor e data de venda
- Ao iniciar a construção, **avisar** (sem bloquear) se ainda houver contrato de
  `PARCELAMENTO_COMPRA` com situação `ATIVO` — o banco costuma exigir o terreno
  quitado para financiar a obra
