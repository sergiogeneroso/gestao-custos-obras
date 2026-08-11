# Gestão de Custos de Obras

Sistema de cadastro e rastreamento financeiro de imóveis e lotes em construção,
registrando todos os custos por etapa e por aportante (quem pagou), com relatório
consolidado final.

## Stack Técnica

| Camada     | Tecnologia                                              |
|------------|---------------------------------------------------------|
| Backend    | Java 25 + Spring Boot 4.1, Maven                        |
| Banco      | PostgreSQL, Flyway (migrações versionadas)              |
| Segurança  | Spring Security (HTTP Basic temporário → JWT futuro)    |
| Auxiliar   | Lombok, Spring DevTools, Bean Validation                |
| Frontend   | Angular + Angular Material (ainda não iniciado)         |

## Arquitetura

**Package by feature (vertical slice)** — cada domínio concentra tudo que é dele
num único pacote. Detalhes em `docs/ARQUITETURA.md`.

Pacote raiz: `com.seegeneroso.gestao_custos_obras`

```
├── imovel/            → ImovelModel, ImovelRepository, ImovelService, ImovelMapper, ImovelController, dto/
├── aportante/         → AportanteModel, AportanteRepository, AportanteService, AportanteMapper, AportanteController, dto/
├── etapaProjeto/      → EtapaProjetoModel, EtapaProjetoRepository, EtapaProjetoService, EtapaProjetoMapper, EtapaProjetoController, dto/
├── despesa/           → DespesaModel, DespesaPagamentoModel, DespesaRepository, DespesaPagamentoRepository, DespesaService, DespesaMapper, DespesaController, dto/
├── orcamentoEtapa/    → OrcamentoEtapaModel, OrcamentoEtapaRepository, OrcamentoEtapaService, OrcamentoEtapaMapper, OrcamentoEtapaController, dto/ (Orçado vs Realizado + Status)
├── relatorio/         → RelatorioService, RelatorioController, dto/ (RF05 — custo por imóvel, custo por m², extrato de aportes, orçado vs realizado + filtros + CSV)
├── auth/              → (a implementar: JWT/RNF01)
└── shared/
    ├── config/        → SecurityConfig, DatabaseCleanupRunner
    ├── enums/         → StatusImovel, TipoImovel
    └── exception/     → RecursoNaoEncontradoException, RegraDeNegocioException, ApiErrorHandler
```

**`shared/` somente para o que é genuinamente transversal a mais de um domínio.**
Nunca criar pacotes por camada (`controller/`, `service/`, `repository/`, `model/`).

## Convenções do Projeto

### Nomenclatura
- Entidades JPA: sufixo `Model` (ex: `ImovelModel`, `DespesaModel`)
- DTOs sempre como `record`; nunca expor a entidade JPA diretamente na API
- DTOs ficam em `dto/` dentro do pacote do domínio
- Mapper: classe dedicada por domínio (ex: `ImovelMapper`)

### Tipos de Dados
- Valores monetários: **sempre** `BigDecimal` no Java e `NUMERIC(14,2)` no PostgreSQL
- **Nunca** usar `double`, `float` ou `Double` para dinheiro

### Persistência
- Soft delete (`ativo BOOLEAN`) em `Imovel` e `Aportante` — nunca `DELETE` físico
  nessas entidades (têm histórico financeiro vinculado)
- Migrations com Flyway: **nunca editar** uma migration já aplicada; sempre criar
  nova (`V2__...`, `V3__...`)
- Nomes de tabelas no banco: `snake_case` (ex: `etapa_projeto`, `despesa_pagamento`)

### Regras de Negócio Críticas
- **RF04 — Rateio**: a soma de `DespesaPagamento.valorPago` **não pode exceder**
  `Despesa.valor`. Validar no service antes de persistir.
- `Imovel` com `ativo = false` não aparece nas listagens nem aceita novas despesas.
- `Aportante` com `ativo = false` não pode ser vinculado a novos pagamentos.
- `Despesa.deletar()` é DELETE físico (comportamento intencional até módulo de auditoria).

### Tratamento de Erros
- `RecursoNaoEncontradoException` → HTTP 404
- `RegraDeNegocioException` → HTTP 422 (Unprocessable Entity)
- Handler global: `ApiErrorHandler` em `shared/exception/`

## Segurança

- **Nunca** commitar credenciais reais em `application.properties`; versionar apenas
  `application.properties.example` com placeholders
- `SecurityConfig` atual: HTTP Basic stateless (temporário) — substituir por JWT
  quando o módulo `auth/` for implementado (RNF01)

## Estado Atual do Projeto

| Domínio          | Status                                             |
|------------------|----------------------------------------------------|
| `imovel`         | CRUD completo + Galeria de Fotos (RF06)            |
| `aportante`      | CRUD completo                                      |
| `etapaProjeto`   | CRUD completo                                      |
| `despesa`        | CRUD completo + PUT + Rateio + Upload Comprovante  |
| `orcamentoEtapa` | CRUD completo + Orçado vs. Realizado + Status      |
| `shared/storage` | Storage local/S3 + Endpoints de Upload/Download    |
| `relatorio`      | CRUD completo + Filtros + Exportação CSV (RF05)    |
| `auth` (JWT)     | A implementar (RNF01)                              |
| Frontend Angular | Setup ainda não iniciado                           |

## Referências Rápidas

- Arquitetura completa e modelo de dados: `docs/ARQUITETURA.md`
- Requisitos funcionais e não funcionais: `docs/REQUISITOS.md`
- Roadmap e pendências: `docs/PROXIMOS-PASSOS.md`
- Decisões de design: `docs/DECISOES.md`
- Modelo do banco: `docs/MODELO-DADOS.md`
