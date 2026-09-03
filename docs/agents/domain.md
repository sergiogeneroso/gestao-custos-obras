# Domain Docs

Como as skills de engenharia devem consumir a documentação de domínio deste
repo ao explorar o código.

Este projeto **não** usa o layout padrão `CONTEXT.md`/`docs/adr/`. Ele já tem
sua própria convenção de documentação em `docs/`, estabelecida antes desta
skill, e é ela que deve ser lida:

## Antes de explorar, leia estes

- **`docs/ARQUITETURA.md`**: arquitetura do sistema
- **`docs/REQUISITOS.md`**: requisitos funcionais
- **`docs/MODELO-DADOS.md`**: modelo de dados / schema — faz o papel de
  glossário de domínio (equivalente ao `CONTEXT.md` de outros projetos)
- **`docs/DECISOES.md`**: log único de ADRs (ex. ADR-020, ADR-023), em vez de
  um arquivo por decisão em `docs/adr/`
- **`docs/PROXIMOS-PASSOS.md`**: roadmap

Esses arquivos são lidos sob demanda conforme a tarefa pedir — não carregam
automaticamente. Não presuma o conteúdo sem ler quando a tarefa depender de
um deles.

## Use o vocabulário do glossário

Quando a saída nomear um conceito de domínio (título de issue, proposta de
refactor, hipótese, nome de teste), use o termo como definido em
`docs/MODELO-DADOS.md` ou nas regras de negócio em `.agents/rules/`. Não
desvie para sinônimos que o projeto evita.

## Sinalize conflitos com ADRs

Se a saída contradiz uma ADR existente em `docs/DECISOES.md`, sinalize
explicitamente em vez de sobrescrever silenciosamente:

> _Contradiz a ADR-020, mas vale reabrir porque…_
