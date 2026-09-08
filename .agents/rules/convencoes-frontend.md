---
paths:
  - "frontend/src/app/**"
---

# Convenções de Tela (Angular)

## Campo vazio em visualização detalhada mostra "—", não some

Em qualquer visualização detalhada — diálogo de detalhe, painel lateral, ficha
de registro — **exiba o rótulo do campo mesmo quando ele está vazio**, com um
travessão no lugar do valor. Nunca envolva o bloco rótulo+valor num `@if` que
some com ele quando não há dado.

```html
<!-- errado: quem nunca preencheu não descobre que o campo existe -->
@if (despesa().observacao) {
  <div><dt>Observação</dt><dd>{{ despesa().observacao }}</dd></div>
}

<!-- certo -->
<div><dt>Observação</dt><dd>{{ despesa().observacao ?? '—' }}</dd></div>
```

O motivo: um campo invisível é indistinguível de um campo inexistente. Isso não
é hipótese — o dono do produto pediu que fosse "adicionado um campo de
observação" que já existia no modelo, na API, no formulário e no painel; ele
nunca o tinha visto porque nenhum lançamento tinha o campo preenchido e o
painel o escondia.

**Exceção: campo condicional ao contexto, não apenas vazio.** "Etapa da
construção" só se aplica a despesa da fase CONSTRUCAO, então continua escondida
numa despesa de lote — mostrar "Etapa —" ali afirma uma ausência em vez de
sinalizar uma lacuna a preencher. O teste: o campo *poderia* ser preenchido
neste registro? Se sim, mostre com travessão; se ele nem se aplica, esconda.

Isso vale para campo de dado. `@if` estrutural — estado de carregamento, lista
vazia, galeria, aba — não tem nada a ver com esta regra.
