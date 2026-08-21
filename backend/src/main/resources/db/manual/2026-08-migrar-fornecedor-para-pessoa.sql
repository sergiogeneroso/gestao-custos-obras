-- Migração manual: o domínio Fornecedor foi eliminado e virou uma marca em Pessoa (ADR-034).
--
-- Rodar UMA VEZ, com o backend já atualizado (o ddl-auto=update cria as colunas novas em pessoa,
-- mas nunca apaga a tabela antiga). Conferir a contagem do passo 3 antes de executar o passo 4.

-- 1) Marca como fornecedora toda pessoa que tinha registro na tabela fornecedor
UPDATE pessoa p
SET fornecedor = TRUE
FROM fornecedor f
WHERE f.pessoa_id = p.id
  AND f.ativo = TRUE;

-- 2) Traz os campos próprios do papel
UPDATE pessoa p
SET area_atuacao = f.area_atuacao,
    observacoes  = f.observacoes
FROM fornecedor f
WHERE f.pessoa_id = p.id
  AND f.ativo = TRUE;

-- 3) Conferência: os dois números precisam bater antes de seguir
SELECT (SELECT COUNT(*) FROM fornecedor WHERE ativo = TRUE) AS fornecedores_ativos,
       (SELECT COUNT(*) FROM pessoa WHERE fornecedor = TRUE) AS pessoas_marcadas;

-- 4) Só depois de conferir o passo 3
-- DROP TABLE fornecedor;
