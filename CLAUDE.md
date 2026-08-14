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
- Use plan mode antes de mexer no cálculo de resultado
  (`relatorio/RelatorioService.java`) ou nas transições de fase/situação do
  imóvel (`imovel/ImovelService.java`) — são os pontos financeiramente mais
  sensíveis do projeto. A regra de custo e as regras do ciclo de vida estão em
  `.agents/rules/regras-negocio-financeiras.md`,
  `.agents/rules/ciclo-vida-imovel.md` e `.agents/rules/contratos-financeiros.md`,
  que carregam sozinhas no escopo delas; o porquê de cada uma está nas ADR-020,
  ADR-023 e ADR-025 em `docs/DECISOES.md`
- Sobre a regra de "Gestão de sessão e contexto" do `AGENTS.md`: no Claude
  Code, isso normalmente significa sugerir rodar a exploração/pesquisa via
  subagente (Task tool), que devolve só o resumo em vez de encher o contexto
  principal, ou sugerir `/clear`/uma sessão nova quando a tarefa atual já
  terminou e a próxima é um domínio não relacionado
