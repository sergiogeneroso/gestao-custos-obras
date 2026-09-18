-- Migração manual: SituacaoContrato ganhou o valor CANCELADO (ADR-043, venda cancelável).
--
-- O ddl-auto=update não atualiza CHECK constraint de coluna ligada a enum quando um valor novo
-- entra no enum Java — a constraint foi criada uma vez, na primeira vez que a tabela nasceu, com
-- os valores que existiam então. Sem rodar isto, ContratoFinanceiroService.cancelarPorVendaDesfeita
-- falha com "new row for relation contrato_financeiro violates check constraint
-- contrato_financeiro_situacao_check". Rodar UMA VEZ.

ALTER TABLE contrato_financeiro DROP CONSTRAINT contrato_financeiro_situacao_check;
ALTER TABLE contrato_financeiro
    ADD CONSTRAINT contrato_financeiro_situacao_check CHECK (situacao IN ('ATIVO', 'QUITADO', 'CANCELADO'));
