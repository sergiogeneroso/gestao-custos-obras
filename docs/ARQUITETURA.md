# Arquitetura

## Padrão: package by feature

Cada domínio (`imovel/`, `aportante/`, `etapaProjeto/`, `despesa/`,
`orcamentoEtapa/`, `relatorio/`, `auth/`) concentra Model, Repository, Service,
Mapper, Controller e `dto/` no mesmo pacote. `shared/` é exceção deliberada:
só o que é genuinamente transversal a 2+ domínios (config, enums,
exceptions, JWT, Flyway). Ver justificativa completa em `docs/DECISOES.md`
(ADR-001).

## Schema do banco (Flyway)

Migrations em `src/main/resources/db/migration/` (V1, V3, V4, V5, V6...).
No Spring Boot 4.1 a `FlywayAutoConfiguration` foi removida, então o
migrador é instanciado por `@Bean` em `shared/config/FlywayConfig.java`,
que roda `migrate()` antes do `EntityManagerFactory` (Hibernate em
`ddl-auto=validate` apenas confere o schema). Detalhes: ADR-012.

A árvore de pacotes atual não está documentada aqui de propósito — rode
`find src/main/java -name "*.java"` ou peça pro Claude explorar, é mais
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

| Domínio       | Base URL               | Status                                    |
|---------------|-------------------------|-------------------------------------------|
| Imóvel        | `/api/imoveis`          | CRUD + galeria de fotos (RF06)             |
| Aportante     | `/api/aportantes`       | CRUD completo                              |
| EtapaProjeto  | `/api/etapas-projeto`   | CRUD completo                              |
| Despesa       | `/api/despesas`         | CRUD + PUT + rateio + upload comprovante   |
| OrcamentoEtapa| `/api/orcamentos-etapa` | CRUD + orçado vs. realizado                |
| Relatório     | `/api/relatorios`       | GET com filtros + exportação CSV (RF05)    |
| Auth          | `/api/auth`             | POST /login emite JWT (RNF01)              |

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
