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

1. **Pergunte ao usuário, se não estiver claro**: o domínio precisa de soft
   delete (`ativo BOOLEAN`, como `Imovel`/`Pessoa`/`Despesa`) ou
   delete físico (como `CategoriaDespesa`)? Precisa de alguma constraint de
   unicidade (ex: nome único, como `CategoriaDespesa.nome`, ou documento
   único, como `Pessoa.documento`)?

2. **Migration** — Flyway está pausado (ADR-013): **não** criar arquivo de
   migration agora, o `{Dominio}Model.java` do passo 3 já basta (Hibernate
   aplica via `ddl-auto=update`). Quando o Flyway for reativado, criar
   `backend/src/main/resources/db/migration/V{next}__criar_{tabela}.sql` (nunca
   editar uma migration existente; ver `.agents/rules/banco-e-migrations.md`)

3. **Criar o pacote** `backend/src/main/java/com/seegeneroso/gestao_custos_obras/{dominio}/`
   com, nesta ordem:
   - `{Dominio}Model.java` — entity JPA, seguir `ImovelModel.java` como modelo
     (Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`)
   - `{Dominio}Repository.java` — `JpaRepository`, incluir
     `findByAtivoTrue()`/`findByIdAndAtivoTrue()` se tiver soft delete
   - `dto/{Dominio}RequestDTO.java` e `dto/{Dominio}ResponseDTO.java` — records,
     validação Bean Validation nos campos obrigatórios
   - `{Dominio}Mapper.java` — `toEntity`, `updateEntityFromDto`, `toResponseDTO`
   - `{Dominio}Service.java` — `@Transactional`, exceptions de
     `shared/exception/` (`RecursoNaoEncontradoException`,
     `RegraDeNegocioException` para violação de regra de negócio)
   - `{Dominio}Controller.java` — `/api/{dominio-plural-em-portugues}`,
     `@Valid`, `ResponseEntity.created()` no POST

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
