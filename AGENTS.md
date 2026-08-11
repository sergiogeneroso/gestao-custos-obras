# Gestão de Custos de Obras — Regras do Projeto

Cadastro de imóveis/lotes com rastreamento de custos de aquisição/construção
por etapa e por aportante, terminando num relatório consolidado.

Este arquivo é lido por qualquer agente de IA (Claude Code, Antigravity).
Regras detalhadas e específicas por tipo de arquivo estão em `.agents/rules/`
(também lidas por ambas as ferramentas — ver `.claude/rules/`, que é um
symlink para essa mesma pasta).

## Regras sempre válidas

- Todo código-fonte (classes, variáveis, métodos, pacotes, endpoints) em
  **português brasileiro**
- Arquitetura: **package by feature** — cada domínio tem seu próprio pacote
  com Model/Repository/Service/Mapper/Controller/dto juntos. Nunca criar
  pacotes por camada (`controller/`, `service/`, `model/` na raiz)
- Pacote raiz do backend: `com.seegeneroso.gestao_custos_obras`
- `shared/` só para o que é genuinamente transversal a 2+ domínios

## Onde encontrar o resto

- Convenções de código Java, regras de negócio financeiras, migrations e
  segurança: `.agents/rules/` (carregadas automaticamente só quando você
  mexe nos arquivos daquele escopo)
- Skill para gerar um novo domínio CRUD: `.agents/skills/gerar-crud-dominio/`
- Arquitetura completa, requisitos, roadmap, decisões e modelo de dados:
  pasta `docs/` (leia sob demanda, não carregam automaticamente)

## Gestão de sessão e contexto

Ao perceber que uma tarefa é grande ou tem uma parte isolável (exploração de
código, pesquisa, leitura de muitos arquivos não relacionados ao objetivo
final), sugira ao usuário — antes de simplesmente seguir consumindo
contexto — se vale mais a pena:
- abrir uma sessão/janela nova pra essa tarefa, ou
- delegar a parte isolável a um subagente,

explicando em uma frase o porquê. Isso é uma sugestão, não uma pausa
obrigatória: se o usuário não responder ou pedir pra seguir, continue
normalmente na sessão atual.

## Testes

Rodar `./mvnw test` antes de considerar uma tarefa finalizada.
