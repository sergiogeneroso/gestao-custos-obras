# Arquitetura

## Padrão de organização: package by feature

O backend segue **package by feature** (vertical slice) em vez de package by
layer. Cada domínio de negócio concentra tudo que é dele — entity, repository,
service, controller, mapper, dto — no mesmo pacote.

```
com.seegeneroso.gestao_custos_obras/
├── GestaoCustosObrasApplication.java
├── imovel/
│   ├── ImovelModel.java           (entity JPA)
│   ├── ImovelRepository.java
│   ├── ImovelService.java
│   ├── ImovelMapper.java
│   ├── ImovelController.java      (→ /api/imoveis)
│   └── dto/
│       ├── ImovelRequestDTO.java
│       └── ImovelResponseDTO.java
├── aportante/
│   ├── AportanteModel.java
│   ├── AportanteRepository.java
│   ├── AportanteService.java
│   ├── AportanteMapper.java
│   ├── AportanteController.java   (→ /api/aportantes)
│   └── dto/
│       ├── AportanteRequestDTO.java
│       └── AportanteResponseDTO.java
├── etapaProjeto/
│   ├── EtapaProjetoModel.java
│   ├── EtapaProjetoRepository.java
│   ├── EtapaProjetoService.java
│   ├── EtapaProjetoMapper.java
│   ├── EtapaProjetoController.java (→ /api/etapas-projeto)
│   └── dto/
│       ├── EtapaProjetoRequestDTO.java
│       └── EtapaProjetoResponseDTO.java
├── despesa/
│   ├── DespesaModel.java
│   ├── DespesaPagamentoModel.java
│   ├── DespesaRepository.java
│   ├── DespesaPagamentoRepository.java
│   ├── DespesaService.java
│   ├── DespesaMapper.java
│   ├── DespesaController.java     (→ /api/despesas)
│   └── dto/
│       ├── DespesaRequestDTO.java
│       ├── DespesaPagamentoRequestDTO.java
│       ├── DespesaResponseDTO.java
│       └── DespesaPagamentoResponseDTO.java
├── relatorio/                      (RelatorioService, RelatorioController, dto/ — RF05)
├── auth/                           (a implementar — RNF01)
└── shared/
    ├── config/
    │   ├── SecurityConfig.java
    │   └── DatabaseCleanupRunner.java
    ├── enums/
    │   ├── StatusImovel.java       (PLANEJAMENTO, CONSTRUCAO, FINALIZADO)
    │   └── TipoImovel.java        (LOTE, IMOVEL)
    └── exception/
        ├── RecursoNaoEncontradoException.java  → 404
        ├── RegraDeNegocioException.java        → 422
        └── ApiErrorHandler.java                (@RestControllerAdvice)
```

**Por que package by feature?**
Mexer numa funcionalidade (ex: Imóvel) fica concentrado num único pacote. Com
5+ domínios, isso evita navegar por 4-5 pastas diferentes (package by layer).
Também facilita eventualmente extrair um domínio para outro serviço.

**`shared/` — exceção deliberada**: só entra ali o que é genuinamente usado por
mais de um domínio. Regra: se o código é específico de um único domínio, ele
fica no pacote daquele domínio, mesmo que pareça "genérico".

## Padrão de cada domínio (CRUD)

Cada domínio segue rigorosamente esta estrutura:

| Componente   | Responsabilidade                                           |
|--------------|------------------------------------------------------------|
| `Model`      | Entity JPA, `@Entity`, anotações Lombok                   |
| `Repository` | Interface `JpaRepository`, queries customizadas            |
| `Service`    | Lógica de negócio, `@Transactional`, validações            |
| `Mapper`     | Conversão Entity ↔ DTO (classe manual, sem MapStruct)      |
| `Controller` | Endpoints REST, `@Valid`, delegação ao Service             |
| `dto/`       | Records `RequestDTO` / `ResponseDTO`                       |

## Endpoints REST

| Domínio       | Base URL               | Operações                    |
|---------------|------------------------|------------------------------|
| Imóvel        | `/api/imoveis`         | POST, GET, GET/{id}, PUT/{id}, DELETE/{id} (soft delete) |
| Aportante     | `/api/aportantes`      | POST, GET, GET/{id}, PUT/{id}, DELETE/{id} (soft delete) |
| EtapaProjeto  | `/api/etapas-projeto`  | POST, GET, GET/{id}, PUT/{id}, DELETE/{id}               |
| Despesa       | `/api/despesas`        | POST, GET, GET/{id}, DELETE/{id}; GET por imóvel         |
| Relatório     | `/api/relatorios`      | GET custo-por-imovel, custo-por-m2, extrato-aportantes, orcado-vs-realizado (filtros + `?format=csv`) |

## Segurança (estado atual)

`SecurityConfig` configura HTTP Basic stateless com CSRF desabilitado. Todos os
endpoints exigem autenticação. Isso é temporário — será substituído por JWT
quando o módulo `auth/` for implementado (RNF01).

## Modelo de dados

Ver `docs/MODELO-DADOS.md` para diagrama completo do schema PostgreSQL.
