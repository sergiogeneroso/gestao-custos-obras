# Arquitetura

## Padrão: package by feature

Cada domínio (`imovel/`, `aportante/`, `etapaProjeto/`, `despesa/`,
`orcamentoEtapa/`, `relatorio/`) concentra Model, Repository, Service,
Mapper, Controller e `dto/` no mesmo pacote. `shared/` é exceção deliberada:
só o que é genuinamente transversal a 2+ domínios (config, enums,
exceptions). Ver justificativa completa em `docs/DECISOES.md` (ADR-001).

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
| Auth          | —                        | A implementar (RNF01)                      |

## Segurança (estado atual)

`SecurityConfig` usa HTTP Basic stateless, CSRF desabilitado, todos os
endpoints exigem autenticação. Temporário até o módulo `auth/` (JWT).
Regras detalhadas: `.agents/rules/seguranca.md`.

## Modelo de dados

Ver `docs/MODELO-DADOS.md` para o schema completo.
