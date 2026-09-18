# 01 — Infraestrutura de teste de integração (banco local)
Type: task
Status: open

Decisões 3, 4 e 5 da spec.

- `backend/src/main/resources/db/manual/criar-banco-teste.sql` com
  `CREATE DATABASE gestao_custos_obras_test;`
- `backend/src/test/resources/application-test.properties`: URL
  `jdbc:postgresql://localhost:5432/gestao_custos_obras_test`,
  `ddl-auto=create-drop`, `show-sql=false`; demais props herdadas do
  `application.properties` (usuário/senha iguais). Comentário de uma linha:
  trocar por Testcontainers quando o projeto usar Docker (ADR-045).
- `GestaoCustosObrasApplicationTests` e `shared/BuscasPaginadasTest` com
  `@ActiveProfiles("test")`. Verificar como `BuscasPaginadasTest` sobe hoje
  e se precisa de `@AutoConfigureTestDatabase(replace = NONE)` para não
  tentar banco embutido. Atenção: Spring Boot 4.1 pode ter mudado o pacote
  de `@DataJpaTest`/`@AutoConfigureTestDatabase`; confirmar no classpath.
- Criar o banco local (rodar o script com `psql` se disponível; a senha do
  postgres vem de onde o `application.properties`/ambiente a define).
- ADR-045 em `docs/DECISOES.md` (formato das ADRs vizinhas, ex. ADR-043):
  banco local de teste agora; Testcontainers adiado porque o Docker não roda
  no ambiente do usuário; gatilho da troca; teste falha sem Postgres (não
  pula) para a proteção não sumir com build verde.
- Item pendente em `docs/PROXIMOS-PASSOS.md`: migrar para Testcontainers
  quando usar Docker (adicionar `spring-boot-testcontainers` +
  `testcontainers-postgresql`, `@ServiceConnection`, apagar script e perfil).
- `AGENTS.md`, seção Testes: Postgres local de pé + rodar uma vez o script do
  banco de teste.
- `.agents/rules/testes-backend.md`: a seção "Controller e Repository" diz
  que o projeto não tem banco de teste — trocar por como escrever teste de
  repository com o perfil `test`.

Pronto quando `./mvnw test` passa com os dois testes rodando contra
`gestao_custos_obras_test` e o banco de dev não é tocado.
