-- Cria o banco usado pelos testes de integração (perfil `test`, ver
-- src/test/resources/application-test.properties). Rodar UMA VEZ por
-- ambiente local, conectado ao Postgres (não a um banco específico), ex.:
--   psql -U postgres -h localhost -f criar-banco-teste.sql
--
-- Nunca aponta para o banco de desenvolvimento (`gestao_custos_obras`) — é
-- um banco à parte, descartável, recriado pelo Hibernate a cada execução de
-- teste (`ddl-auto=create-drop`). Testcontainers substitui isso quando o
-- projeto passar a usar Docker (ADR-045).

CREATE DATABASE gestao_custos_obras_test;
