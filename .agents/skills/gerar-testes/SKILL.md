---
name: gerar-testes
description: Gera testes unitários focados em comportamento para um arquivo (ou para os arquivos alterados na branch) seguindo as guias de teste do projeto. Uso - /gerar-testes <arquivo> ou /gerar-testes --alterados
disable-model-invocation: true
---

# Gerar testes

## 1. Carregar as guias

As rules de teste só carregam sozinhas quando um arquivo de teste já é lido;
para um teste novo, leia-as agora:

- `.agents/rules/testes-core.md` — sempre
- `.agents/rules/testes-backend.md` ou `testes-frontend.md` — conforme o alvo
- `.agents/rules/testes-dominio-financeiro.md` — se o alvo estiver em
  `relatorio`, `contratoFinanceiro`, `imovel` ou `despesa`, mais a rule de
  negócio que ela aponta

Pronto quando as guias do alvo estão lidas.

## 2. Resolver os alvos

- `<arquivo>`: esse arquivo
- `--alterados`: `git diff --name-only master...HEAD` mais as mudanças não
  commitadas, filtrando `.java` de `backend/src/main` e `.ts` de
  `frontend/src` que não sejam `.spec.ts`

Para cada alvo, decida pela guia do tipo se ele **merece** teste (Controller
que só delega, service HTTP que só repassa, mapeamento sem regra não
merecem). Pronto quando cada alvo está marcado "testar" ou "pular — motivo".

## 3. Escrever

Por alvo marcado "testar":

1. Leia o arquivo e o teste existente dele, se houver — ampliar o existente,
   reaproveitando seus construtores de dados, vem antes de criar outro
2. Liste as regras de comportamento que ele implementa; cada uma vira um
   teste (máximo ~10 novos por arquivo)
3. Escreva os testes seguindo as guias

## 4. Verificar

Rode a suíte do lado afetado (comandos em `testes-core.md`). Pronto quando
todos passam **e** cada teste de regra novo foi visto vermelho ao quebrar a
regra no código (e o código foi restaurado).

## 5. Relatar

Por alvo: testes criados (nome de cada um), alvos pulados com o motivo, e
qualquer regra que pareceu sem cobertura mas ficou fora do escopo.
