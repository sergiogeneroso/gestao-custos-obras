-- Migração manual: ContratoFinanceiroModel.imovel (FK única) vira ContratoFinanceiroModel.imoveis
-- (N:N via ContratoImovelModel/tabela contrato_imovel), porque um único PARCELAMENTO_COMPRA ou
-- PARCELAMENTO_VENDA pode cobrir vários lotes ao mesmo tempo (ADR-047).
--
-- Rodar em duas etapas, com o backend já subido pelo menos uma vez depois do deploy desta mudança
-- (o ddl-auto=update cria a tabela contrato_imovel antes do passo 1 rodar).

-- 1) Migra cada contrato existente para uma linha em contrato_imovel, com a fatia = o valor
--    contratado inteiro (era 1 contrato : 1 lote antes da ADR-047, então a fatia é 100%).
INSERT INTO contrato_imovel (contrato_id, imovel_id, valor_alocado)
SELECT id, imovel_id, valor_contratado
FROM contrato_financeiro
WHERE imovel_id IS NOT NULL;

-- 2) Conferência: toda linha de contrato_financeiro precisa ter virado uma linha em contrato_imovel
SELECT COUNT(*) AS contratos_sem_alocacao
FROM contrato_financeiro cf
WHERE NOT EXISTS (SELECT 1 FROM contrato_imovel ci WHERE ci.contrato_id = cf.id);

-- 3) Só depois de conferir que o passo 2 deu zero
-- ALTER TABLE contrato_financeiro DROP COLUMN imovel_id;
