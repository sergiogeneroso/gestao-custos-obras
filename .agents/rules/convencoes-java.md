---
paths:
  - "backend/src/main/java/**/*.java"
---

# Convenções de Código Java

- Entidades JPA: sufixo `Model` (ex: `ImovelModel`, `PessoaModel`)
- DTOs: sempre `record`, nunca classe mutável; sufixo `RequestDTO`/`ResponseDTO`,
  dentro de `dto/` no pacote do domínio
- Mapper: classe manual dedicada por domínio (sem MapStruct)
- `@Transactional` em todo método de service; `readOnly = true` para leituras
- `@RequiredArgsConstructor` + campos `final` para injeção de dependência
- Nunca expor a entidade JPA diretamente na API — sempre via DTO
