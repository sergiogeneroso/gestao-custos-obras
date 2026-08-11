@AGENTS.md

## Claude Code

- Para detalhes de arquitetura, requisitos, roadmap, decisões (ADRs) e o
  schema do banco, leia sob demanda os arquivos em `docs/` conforme a tarefa
  pedir — eles não carregam automaticamente, então não presuma o conteúdo
  sem ler quando a tarefa depender de um deles:
  - `docs/ARQUITETURA.md`
  - `docs/REQUISITOS.md`
  - `docs/PROXIMOS-PASSOS.md`
  - `docs/DECISOES.md`
  - `docs/MODELO-DADOS.md`
- Ao criar um novo domínio de negócio (ex. `fornecedor`), use a skill
  `gerar-crud-dominio` em vez de escrever o CRUD do zero
- Use plan mode antes de mexer em regras de rateio de despesa
  (`despesa/DespesaService.java`) — é a regra de negócio mais sensível do
  projeto financeiramente
