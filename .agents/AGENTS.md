# Regras do Projeto — Gestão de Custos de Obras

## Idioma

- Todo código-fonte (classes, variáveis, métodos, enums) deve ser escrito em
  **português brasileiro**, incluindo nomes de pacotes, DTOs e endpoints.
- Mensagens de erro retornadas pela API também em português.
- Comentários de código e documentação em português.

## Arquitetura Obrigatória

- **Package by feature (vertical slice)**: cada domínio de negócio tem seu próprio
  pacote contendo entity, repository, service, mapper, controller e `dto/`.
- **Nunca** criar pacotes por camada (`controller/`, `service/`, `repository/`,
  `model/` na raiz). Se precisar de um novo domínio, crie um pacote no nível do
  domínio (ex: `relatorio/`, `auth/`).
- `shared/` é exclusivo para código genuinamente transversal a 2+ domínios (enums,
  exceptions, config). Nunca colocar lógica de negócio ali.

## Convenções de Código Java

- Entidades JPA: sufixo `Model` (ex: `ImovelModel`, `AportanteModel`)
- DTOs: sempre `record`, nunca classes mutáveis; sufixo `RequestDTO`/`ResponseDTO`
- Mapper: classe manual dedicada por domínio (sem MapStruct por enquanto)
- Valores monetários: `BigDecimal` (Java) + `NUMERIC(14,2)` (PostgreSQL). Proibido
  `double`/`float`/`Double` para dinheiro.
- Soft delete (`ativo BOOLEAN`) em entidades com histórico financeiro (`Imovel`,
  `Aportante`). Usar `findByAtivoTrue()` / `findByIdAndAtivoTrue()` nas queries.
- `@Transactional` em todos os métodos de service; `readOnly = true` para leituras.
- `@RequiredArgsConstructor` + `final` fields para injeção de dependência (Lombok).

## Banco de Dados e Migrations

- Flyway com migrations versionadas em `src/main/resources/db/migration/`.
- **Nunca editar** uma migration já aplicada; sempre criar nova com próximo número.
- Nomes de tabelas e colunas em `snake_case`.
- Foreign keys explícitas com `REFERENCES`. `ON DELETE RESTRICT` em relações
  financeiras (ex: `aportante` em `despesa_pagamento`).

## API REST

- Prefixo `/api/` em todos os endpoints.
- Pluralizado em português: `/api/imoveis`, `/api/aportantes`, `/api/etapas-projeto`,
  `/api/despesas`.
- Respostas de erro padronizadas via `ApiErrorHandler`:
  - `RecursoNaoEncontradoException` → 404
  - `RegraDeNegocioException` → 422
  - `MethodArgumentNotValidException` → 400

## Segurança

- **Nunca** commitar credenciais reais. Usar variáveis de ambiente.
- O `application.properties` atual é local; versionar apenas
  `application.properties.example` com placeholders.

## Regras de Negócio que Não Devem Ser Quebradas

1. A soma de `DespesaPagamento.valorPago` para uma `Despesa` **não pode exceder**
   `Despesa.valor` — validar no service.
2. Entidades inativas (`ativo = false`) não devem aparecer em listagens padrão.
3. Não permitir vincular um `Aportante` inativo a novos pagamentos.
4. `EtapaProjeto` é um catálogo global (não por imóvel) — as etapas são cadastradas
   uma vez e reutilizadas.

## Testes

- Rodar com `./mvnw test` antes de considerar uma tarefa finalizada.
- Testes unitários no service, testes de integração no controller (quando existirem).

## Documentação do Projeto

Os seguintes arquivos devem ser mantidos atualizados conforme o projeto evolui:

- `CLAUDE.md` — visão geral e contexto para assistentes de IA
- `docs/ARQUITETURA.md` — estrutura de pacotes e modelo de dados
- `docs/REQUISITOS.md` — requisitos funcionais e não funcionais
- `docs/PROXIMOS-PASSOS.md` — roadmap e status de implementação
- `docs/DECISOES.md` — registros de decisões arquiteturais (ADRs)
- `docs/MODELO-DADOS.md` — diagrama textual do schema do banco
