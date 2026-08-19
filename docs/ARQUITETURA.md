# Arquitetura

## Estrutura do repositório

Monorepo: `backend/` (este projeto Spring Boot) e `frontend/` (Angular) como
pastas irmãs na raiz. Ver ADR-015 em `docs/DECISOES.md`. Este documento
descreve só o `backend/`; o frontend tem sua própria doc em
`docs/FRONTEND.md`.

## Padrão: package by feature

Cada domínio (`imovel/`, `pessoa/`, `categoriaDespesa/`,
`despesa/`, `contratoFinanceiro/`, `orcamentoCategoria/`, `relatorio/`, `auth/`)
concentra Model, Repository, Service, Mapper, Controller e `dto/` no mesmo
pacote. `shared/` é exceção deliberada:
só o que é genuinamente transversal a 2+ domínios (config, enums,
exceptions, JWT, Flyway). Ver justificativa completa em `docs/DECISOES.md`
(ADR-001).

## Schema do banco (Flyway — pausado, ver ADR-013)

Flyway está **pausado** enquanto a modelagem do MVP não fecha: schema gerido
direto pelo Hibernate via `ddl-auto=update`, e `FlywayConfig` está sem
`@Configuration` (beans não registrados). As migrations antigas em
`backend/src/main/resources/db/migration/` (V1, V3, V4, V5, V6...) ficam inertes, não
foram apagadas. Quando o Flyway for reativado: religar `@Configuration` em
`shared/config/FlywayConfig.java`, voltar `ddl-auto=validate` e consolidar as
migrations antigas numa `V1` baseline única. No Spring Boot 4.1 a
`FlywayAutoConfiguration` foi removida do framework — por isso, quando
reativado, o migrador volta a ser instanciado manualmente por esse `@Bean`,
que roda `migrate()` antes do `EntityManagerFactory`. Detalhes: ADR-012/013.

A árvore de pacotes atual não está documentada aqui de propósito — rode
`find backend/src/main/java -name "*.java"` ou peça pro Claude explorar, é mais
confiável que manter isso sincronizado manualmente.

## Padrão de cada domínio (CRUD)

| Componente   | Responsabilidade                                       |
|--------------|----------------------------------------------------------|
| `Model`      | Entity JPA                                               |
| `Repository` | `JpaRepository`, queries customizadas                    |
| `Service`    | Lógica de negócio, `@Transactional`, validações          |
| `Mapper`     | Conversão Entity ↔ DTO (manual, sem MapStruct)           |
| `Controller` | Endpoints REST, `@Valid`, delegação ao Service            |
| `dto/`       | Records `RequestDTO` / `ResponseDTO`                      |

Use a skill `gerar-crud-dominio` para criar um novo domínio seguindo esse
padrão automaticamente.

## Endpoints REST

Alvo do reescopo de Ago 2026 (ADR-019 a ADR-029):

| Domínio            | Base URL                    | Escopo                                        |
|--------------------|-----------------------------|-----------------------------------------------|
| Imóvel             | `/api/imoveis`              | CRUD + fotos + documentos + `PATCH` fase/situação |
| Pessoa             | `/api/pessoas`              | CRUD + marca de fornecedor (ADR-021/034)       |
| CategoriaDespesa   | `/api/categorias-despesa`   | CRUD (substitui EtapaProjeto)                  |
| Despesa            | `/api/despesas`             | CRUD + anexos tipados, sem rateio              |
| ContratoFinanceiro | `/api/contratos-financeiros`| CRUD + parcelas + quitação + documentos        |
| Relatório          | `/api/relatorios`           | Resultado, fornecedor, extrato, carteira + CSV |
| OrcamentoCategoria | `/api/orcamentos-categoria` | Existe no código, **fora do MVP** (ADR-029)    |
| Auth               | `/api/auth`                 | POST /login emite JWT (RNF01)                  |

As invariantes financeiras e as do ciclo de vida do imóvel não vivem aqui: estão
em `.agents/rules/regras-negocio-financeiras.md`, `ciclo-vida-imovel.md` e
`contratos-financeiros.md`, que carregam automaticamente ao editar os pacotes
correspondentes.

## Segurança (estado atual)

`SecurityConfig` usa **JWT stateless** (jjwt), CSRF desabilitado.
`/api/auth/login` é público; demais endpoints exigem `Authorization: Bearer
<token>`. `JwtService` gera/valida o token (HMAC-SHA256) e
`JwtAuthenticationFilter` extrai o `Authentication` (authorities
`ROLE_<role>`). `JwtAuthenticationEntryPoint` devolve 401 adequado. Senha
como hash BCrypt (`BCryptPasswordEncoder`).

A role viaja no token; **regras `.hasRole(...)` por domínio ficam pendentes
para a fase 2**, quando os outros tipos de role forem definidos. Seed admin
inicial via `SeedUsuarioAdminRunner` (idempotente) — admin@gestao.local /
admin123 (role ADMIN). Regras detalhadas: `.agents/rules/seguranca.md`.

## Modelo de dados

Ver `docs/MODELO-DADOS.md` para o schema completo.
