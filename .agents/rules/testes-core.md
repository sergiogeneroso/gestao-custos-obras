---
paths:
  - "backend/src/test/**"
  - "frontend/src/**/*.spec.ts"
---

# Testes — regras base (backend e frontend)

Complementos por tipo: `testes-backend.md` e `testes-frontend.md`. Por caso:
`testes-dominio-financeiro.md`.

## O que testar

- **Comportamento de negócio pela interface pública** — o método do Service,
  a função exportada, a diretiva no DOM. O teste descreve o que o sistema faz,
  e sobrevive a renomear variável ou extrair método privado
- Prioridade: regra financeira e de ciclo de vida > validação de entrada >
  formatação/utilitário. Tela e CRUD trivial só quando houver lógica própria
- Poucos testes focados: um teste por regra, **no máximo ~10 por arquivo
  novo**. Se a classe pede mais, provavelmente são duas responsabilidades

## O que deixar de fora

O framework já é testado por quem o escreveu. Deixe de fora: getter/setter,
Lombok, mapeamento campo a campo sem regra, injeção de dependência do Spring
ou do Angular, "o componente foi criado" (`toBeTruthy()`), "o repository foi
chamado" quando o resultado já prova isso.

## Nome e forma do teste

- Nome em português, frase que afirma a regra, em camelCase no Java e texto
  livre no `it(...)`: `saldoDevedorNaoEntraNoCusto`,
  `it('cai em "outro" para extensão desconhecida')`
- Corpo em três blocos separados por linha em branco: montar → agir →
  verificar
- **Valor esperado é literal** (`"100050"`), calculado à mão a partir da
  regra — nunca recomputado no teste com a mesma conta do código. Quando a
  conta não é óbvia, um comentário de uma linha mostra a soma
  (`// 2000 + 200000 + 5000 de despesas + 30 de juros`)

## Dados de teste

- Construtores de dados como **métodos privados no fim da própria classe de
  teste** (`imovel(...)`, `despesa(...)`, `parcela(...)`) com só os
  parâmetros que variam entre os testes; o resto recebe um padrão válido.
  Arquivo de fixture compartilhado só quando o segundo arquivo de teste
  precisar do mesmo construtor
- Data fixa (`LocalDate.of(2026, 1, 10)`) quando a regra compara datas;
  `LocalDate.now()` só onde a data é irrelevante

## Comentário de cabeçalho

Classe de teste que trava uma regra de `.agents/rules/` ou uma ADR abre com
um comentário curto dizendo qual regra cobre e por quê — é o que impede
alguém de apagar o teste achando que é redundante. Ver
`RelatorioServiceTest` e `ImovelServiceTest`.

## Antes de dar por pronto

Rode a suíte e veja os testes novos passarem — e, para teste de regra,
confirme que ele fica vermelho se a regra for quebrada (inverta a condição
no código, rode, desfaça).

- Backend: `cd backend && ./mvnw test`
- Frontend: `cd frontend && npx ng test --watch=false`
