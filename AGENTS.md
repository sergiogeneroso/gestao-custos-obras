# Gestão de Custos de Obras — Regras do Projeto

Acompanhamento do **resultado financeiro de cada imóvel do começo ao fim**:
compra do lote, custos ao longo de todo o ciclo de vida (lote → construção →
casa, quando houver obra) e venda, terminando num relatório de resultado por
imóvel e da carteira. Ver ADR-019 em `docs/DECISOES.md`.

O que fica fora desse eixo evolui como módulo posterior — e "módulo" aqui
significa apenas um novo pacote em package by feature, sem infraestrutura de
modularidade (ADR-029).

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

- Convenções de código Java, regras de negócio financeiras, ciclo de vida do
  imóvel, contratos financeiros, migrations e segurança: `.agents/rules/`
  (carregadas automaticamente só quando você mexe nos arquivos daquele escopo).
  As invariantes financeiras e as do ciclo de vida vivem lá de propósito —
  `docs/` é lido sob demanda e não protegeria nada sozinho
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

## Estrutura do repositório

Monorepo: `backend/` (Spring Boot/Java) e `frontend/` (Angular) na raiz,
lado a lado. Ver ADR-014/015 em `docs/DECISOES.md`.

## Testes

Rodar `cd backend && ./mvnw test` antes de considerar uma tarefa de backend
finalizada.
