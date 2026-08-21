-- Migração manual: o identificador do imóvel passa a ser único sem depender do case.
--
-- O ddl-auto=update não cria índice funcional, e `unique = true` na coluna criaria uma constraint
-- case-sensitive — que não é a regra ("LOTE-01" e "lote-01" são o mesmo imóvel). Rodar UMA VEZ.
-- Quando o Flyway for reativado (ADR-029), este índice entra no baseline V1.

-- 1) Conferir que não há duplicado antes de criar o índice. Precisa devolver 0 linhas.
SELECT lower(identificador) AS identificador, count(*)
FROM imovel
GROUP BY 1
HAVING count(*) > 1;

-- 2) Criar o índice. Se o passo 1 devolveu alguma linha, corrigir os identificadores antes.
CREATE UNIQUE INDEX ux_imovel_identificador_lower ON imovel (lower(identificador));
