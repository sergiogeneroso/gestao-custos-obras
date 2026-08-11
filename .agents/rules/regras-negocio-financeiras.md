---
paths:
  - "src/main/java/**/despesa/**"
  - "src/main/java/**/aportante/**"
  - "src/main/java/**/imovel/**"
---

# Regras de Negócio Financeiras (não quebrar)

- A soma de `DespesaPagamento.valorPago` para uma `Despesa` **não pode
  exceder** `Despesa.valor` — validar no service antes de persistir
- Valores monetários: **sempre** `BigDecimal` (Java) + `NUMERIC(14,2)`
  (PostgreSQL). Proibido `double`/`float`/`Double` para dinheiro
- Soft delete (`ativo BOOLEAN`) em `Imovel` e `Aportante` — nunca `DELETE`
  físico nessas entidades. Usar `findByAtivoTrue()` / `findByIdAndAtivoTrue()`
- `Imovel` com `ativo = false` não aparece em listagens nem aceita novas
  despesas
- `Aportante` com `ativo = false` não pode ser vinculado a novos pagamentos
- `EtapaProjeto` é catálogo global (não por imóvel) — cadastrada uma vez,
  reutilizada em qualquer imóvel
- `DespesaService.deletar()` é DELETE físico — comportamento intencional até
  existir módulo de auditoria (ver `docs/DECISOES.md`)
