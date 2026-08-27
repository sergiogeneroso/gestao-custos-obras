-- Normaliza documento e telefone das pessoas já cadastradas.
--
-- A partir da validação de CPF/CNPJ, o backend grava documento e telefone sem
-- pontuação (ver Documentos.java) — a máscara passou a ser só de exibição. Os
-- registros anteriores foram gravados como o usuário digitou, então o mesmo CPF
-- com e sem pontos existe hoje como duas linhas distintas para o
-- `existsByDocumento`.
--
-- Rodar UMA vez, depois do deploy da validação. Conferir o SELECT antes:

-- 1) O que vai mudar:
-- SELECT id, nome, documento, telefone,
--        upper(regexp_replace(documento, '[^A-Za-z0-9]', '', 'g')) AS documento_novo,
--        regexp_replace(telefone, '\D', '', 'g') AS telefone_novo
--   FROM pessoa
--  WHERE documento <> upper(regexp_replace(documento, '[^A-Za-z0-9]', '', 'g'))
--     OR telefone  <> regexp_replace(telefone, '\D', '', 'g');

-- 2) Duplicidades que a normalização criaria (resolver à mão ANTES do UPDATE,
--    porque documento é UNIQUE e o UPDATE falharia no meio):
-- SELECT upper(regexp_replace(documento, '[^A-Za-z0-9]', '', 'g')) AS documento_novo,
--        count(*), array_agg(id)
--   FROM pessoa
--  GROUP BY 1 HAVING count(*) > 1;

UPDATE pessoa
   SET documento = upper(regexp_replace(documento, '[^A-Za-z0-9]', '', 'g')),
       telefone  = NULLIF(regexp_replace(coalesce(telefone, ''), '\D', '', 'g'), '');
