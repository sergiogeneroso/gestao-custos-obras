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

## Campo de formulário nasce com máscara, visível já na digitação

Todo campo com formato conhecido usa a diretiva de `shared/` correspondente. A
máscara aparece **enquanto se digita**, não só no blur — o dono do produto pediu
isso para todos os cadastros, e as diretivas já cuidam do cursor
(`shared/mascara/caret.ts`).

| Campo | Template | O controle guarda |
|---|---|---|
| Dinheiro | `<input matInput appMoeda formControlName="…">` | `number \| null` |
| Data | `<input matInput [matDatepicker]="p" appMascaraData formControlName="…">` + `mat-datepicker-toggle` | `Date \| null` |
| Telefone, CEP, CNO | `<input matInput appMascara="telefone" formControlName="…">` (idem `cep`, `cno`) | só dígitos |
| CPF/CNPJ | `[appMascara]="mascaraDocumento(tipo)"` — muda com o tipo da pessoa | só letras e dígitos |
| UF | `mat-select` sobre `UFS` (`shared/uf.ts`); `UF_PADRAO` (MG) só em registro novo | sigla |

- **Moeda é caixa registradora**: só dígitos, os dois últimos são os centavos, e
  campo apagado é `null` — vazio e zero são coisas diferentes no relatório.
- **Data é sempre `matDatepicker` + `appMascaraData`**, inclusive em grade densa
  fora de `mat-form-field` (envio de documento do imóvel). É o que mantém o
  formato `dd/mm/aaaa` e o parse do `DataPtBrAdapter` iguais no sistema todo.
- **Máscara fixa nova** (formato que se repete entre telas) entra no catálogo
  `shared/mascara/catalogo.ts` e é pedida por nome no template.
- **Valor gravado limpo se exibe formatado** fora do formulário também — use o
  `formatar…` da própria máscara (`formatarTelefone`, `formatarCep`, `exibirCno`).
- **Código de formato livre fica em texto puro**: matrícula, inscrição municipal,
  alvará, ART e identificador variam por cartório, prefeitura e CREA, e uma
  máscara ali impediria digitar o número real. Área e contadores (quartos,
  vagas) seguem `type="number"`.
