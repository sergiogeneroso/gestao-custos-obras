---
name: gerar-crud-dominio
description: Use esta skill ao criar um novo domínio de negócio no backend (ex. "categoria", "contrato", "orçamento") que precisa de um CRUD completo seguindo o padrão já estabelecido no projeto (package by feature com Model/Repository/Service/Mapper/Controller/dto). Não use para endpoints de agregação/relatório, nem para adicionar um único campo a um domínio existente.
---

# Gerar CRUD de um novo domínio

Vários domínios do projeto seguem exatamente o mesmo padrão: `imovel/`,
`pessoa/`, `categoriaDespesa/`, `contratoFinanceiro/`. Use `imovel/` como
referência canônica — leia esses arquivos antes de gerar o novo domínio.
`categoriaDespesa/` é o exemplo mais simples (poucos campos, delete físico) se
preferir um ponto de partida menor.

## Passo a passo

1. **Exclusão lógica é o padrão** (ADR-040): todo domínio novo nasce com o
   `@Embeddable` compartilhado `shared/exclusao/ExclusaoLogica.java`
   (`ativo`, `motivoExclusao`, `excluidoEm`, `excluidoPor`) em vez de um
   `Boolean ativo` solto. Delete físico só para catálogo global sem valor
   histórico (como `CategoriaDespesa`) — **pergunte ao usuário se não estiver
   claro** que o domínio é desse tipo. Também pergunte: precisa de alguma
   constraint de unicidade (ex: nome único, como `CategoriaDespesa.nome`, ou
   documento único, como `Pessoa.documento`)? O domínio novo cascateia a
   exclusão de algum pai (ex: uma parcela cascateia com o contrato) — nesse
   caso o filho só ganha `ativo=false` pela cascata, sem endpoint `DELETE`
   próprio (ver `ParcelaContratoModel`/`OrcamentoCategoriaModel`)?

2. **Migration** — Flyway está pausado (ADR-013): **não** criar arquivo de
   migration agora, o `{Dominio}Model.java` do passo 3 já basta (Hibernate
   aplica via `ddl-auto=update`). Quando o Flyway for reativado, criar
   `backend/src/main/resources/db/migration/V{next}__criar_{tabela}.sql` (nunca
   editar uma migration existente; ver `.agents/rules/banco-e-migrations.md`)

3. **Criar o pacote** `backend/src/main/java/com/seegeneroso/gestao_custos_obras/{dominio}/`
   com, nesta ordem:
   - `{Dominio}Model.java` — entity JPA, seguir `ImovelModel.java` como modelo
     (Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`).
     Com soft delete: `@Embedded private ExclusaoLogica exclusao = new
     ExclusaoLogica();` + getter manual que nunca devolve `null` (ver
     `ImovelModel.getExclusao()`) — obrigatório porque o `@ManyToOne`
     (`excluidoPor`) dentro do embeddable faz o Hibernate devolver `null` em
     vez do objeto vazio. Se a entidade já não tinha soft delete antes
     (tabela existente), sobrescrever a coluna `ativo` com
     `columnDefinition = "BOOLEAN DEFAULT TRUE"` via `@AttributeOverride`
     (`ddl-auto=update` não aplica `ADD COLUMN NOT NULL` sem default numa
     tabela com linhas — ver `ContratoFinanceiroModel` como exemplo)
   - `{Dominio}Repository.java` — `JpaRepository`. Com soft delete, os
     métodos de busca (`findByAtivoTrue()`, `findByIdAndAtivoTrue()`, etc.)
     viram `@Query` JPQL contra `x.exclusao.ativo = true` em vez de
     query-method derivado, mas **mantendo o mesmo nome de método** — é o
     que evita qualquer consumidor (inclusive `RelatorioService`) precisar
     mudar quando a entidade ganha o embeddable (ver `ImovelRepository`)
   - `dto/{Dominio}RequestDTO.java` e `dto/{Dominio}ResponseDTO.java` — records,
     validação Bean Validation nos campos obrigatórios
   - `{Dominio}Mapper.java` — `toEntity`, `updateEntityFromDto`, `toResponseDTO`
     (com soft delete, `entity.getExclusao().getAtivo()` no `ResponseDTO`)
   - `{Dominio}Service.java` — `@Transactional`, exceptions de
     `shared/exception/` (`RecursoNaoEncontradoException`,
     `RegraDeNegocioException` para violação de regra de negócio). Método de
     exclusão chama-se `excluir(Long id, String motivo)`, nunca `inativar`
     nem `deletar` — grava via `entity.getExclusao().excluir(motivo,
     usuarioAutenticadoService.usuarioAtual())` (injetar
     `shared/auth/UsuarioAutenticadoService`)
   - **Auditoria é obrigatória em `criar`/`atualizar`/`excluir`** (ADR-042,
     ver `.agents/rules/auditoria.md`): injetar
     `shared/auditoria/AuditoriaService` e chamar
     `auditoriaService.registrar("{Dominio}", id, OperacaoAuditoria.CRIACAO|EDICAO|EXCLUSAO,
     estadoAnterior, estadoNovo)`, sempre com o `ResponseDTO` (nunca a
     entidade) como estado. Em `atualizar`/`excluir`, mapear `estadoAnterior`
     para DTO **antes** de aplicar qualquer mutação na entidade — inclusive
     antes da mutação de `ExclusaoLogica.excluir(...)`
   - `{Dominio}Controller.java` — `/api/{dominio-plural-em-portugues}`,
     `@Valid`, `ResponseEntity.created()` no POST. `DELETE /{id}` recebe
     `@Valid @RequestBody ExclusaoRequestDTO dto` (`shared/exclusao/`) e
     chama `service.excluir(id, dto.motivo())`

4. **Atualizar a documentação** na mesma tarefa:
   - `docs/MODELO-DADOS.md` — adicionar a tabela no diagrama Mermaid e na
     seção de detalhes
   - `docs/ARQUITETURA.md` — adicionar o domínio na tabela de endpoints
   - `docs/PROXIMOS-PASSOS.md` — marcar como feito
   - `CLAUDE.md`/`AGENTS.md` — **não** precisa atualizar a menos que o
     domínio introduza uma convenção nova (regra path-scoped nova, por
     exemplo)

5. **Rodar `cd backend && ./mvnw test`** antes de considerar a tarefa concluída.

## O que NUNCA fazer

- Não criar pacote `model/`, `service/`, `controller/` separados — tudo
  dentro do pacote do domínio (package by feature)
- Não usar `double`/`float` para qualquer valor monetário
- Não esquecer `ON DELETE RESTRICT` se a nova tabela referenciar `pessoa`
  ou outra entidade com histórico financeiro
