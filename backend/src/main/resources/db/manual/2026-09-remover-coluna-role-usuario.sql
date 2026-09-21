-- Migração manual: UsuarioModel.role (String solta) virou UsuarioModel.perfil (FK pra
-- PerfilModel), como parte do RBAC por domínio (ADR-046).
--
-- Rodar UMA VEZ, com o backend já subido pelo menos uma vez (o ddl-auto=update cria a coluna
-- usuario.perfil_id e o PerfilSeedRunner já preencheu todo usuário existente com o perfil
-- "Administrador" — a conferência do passo 1 precisa dar zero antes de seguir).

-- 1) Conferência: nenhum usuário pode estar sem perfil atribuído
SELECT COUNT(*) AS usuarios_sem_perfil FROM usuario WHERE perfil_id IS NULL;

-- 2) Só depois de conferir que o passo 1 deu zero
-- ALTER TABLE usuario DROP COLUMN role;
