# Decisões Arquiteturais (ADRs)

## ADR-001 — Package by feature em vez de package by layer

**Data**: Agosto 2025
**Status**: Aceita

**Contexto**: Com 5+ domínios (Imóvel, Aportante, EtapaProjeto, Despesa, Relatório, Auth), organizar por camada (controller/, service/, repository/, model/) espalharia cada funcionalidade por 4-5 pastas.

**Decisão**: Adotar package by feature (vertical slice). Cada domínio tem seu pacote com entity, repository, service, mapper, controller e dto/ juntos. `shared/` é exceção para código transversal.

**Consequências**: Navegação mais intuitiva por domínio. Facilita extração futura para microsserviços.

---

## ADR-002 — Soft delete para Imóvel e Aportante

**Data**: Agosto 2025
**Status**: Aceita

**Contexto**: Imóveis e aportantes podem ter despesas/pagamentos vinculados. DELETE físico quebraria integridade referencial ou exigiria cascata perigosa.

**Decisão**: Campo `ativo BOOLEAN DEFAULT true`. Listagens usam `findByAtivoTrue()`. DELETE no controller chama `inativar()`.

**Consequências**: Dados nunca são perdidos. Despesas existentes continuam referenciando entidades inativadas.

---

## ADR-003 — Nome definitivo: Aportante (não Envolvido)

**Data**: Agosto 2025
**Status**: Aceita

**Contexto**: Os docs iniciais usavam "Envolvido" mas o termo é genérico demais. "Aportante" reflete melhor quem aporta recursos financeiros.

**Decisão**: Pacote `aportante/`, entity `AportanteModel`, tabela `aportante`.

---

## ADR-004 — Nome definitivo: EtapaProjeto (não Etapa)

**Data**: Agosto 2025
**Status**: Aceita

**Contexto**: "Etapa" é genérico e colide com nomes comuns em Java. "EtapaProjeto" é mais descritivo e evita ambiguidade.

**Decisão**: Pacote `etapaProjeto/`, entity `EtapaProjetoModel`, tabela `etapa_projeto`.

---

## ADR-005 — EtapaProjeto como catálogo global

**Data**: Agosto 2025
**Status**: Aceita

**Contexto**: Etapas de construção (Fundação, Alvenaria, Acabamento) se repetem em todos os imóveis.

**Decisão**: `etapa_projeto` é tabela independente, sem FK para `imovel`. Despesas vinculam imóvel e etapa separadamente.

**Consequências**: Etapas cadastradas uma vez e reutilizadas. Não é possível ter etapas exclusivas de um imóvel (trade-off aceito).

---

## ADR-006 — DespesaPagamento separado de Despesa

**Data**: Agosto 2025
**Status**: Aceita

**Contexto**: RF04 exige tanto "um aportante pagou tudo" quanto "dividir entre sócios" (rateio).

**Decisão**: Tabela `despesa_pagamento` com FK para `despesa` e `aportante`. Uma despesa tem N pagamentos. Soma dos `valor_pago` não pode exceder `despesa.valor`.

**Consequências**: Flexibilidade total no rateio. Validação obrigatória no service.

---

## ADR-007 — BigDecimal para valores monetários

**Data**: Agosto 2025
**Status**: Aceita

**Contexto**: `double`/`float` causam erros de arredondamento inaceitáveis para valores financeiros.

**Decisão**: `BigDecimal` no Java, `NUMERIC(14,2)` no PostgreSQL. `double`/`float`/`Double` proibidos para dinheiro.

---

## ADR-008 — Mapper manual (sem MapStruct)

**Data**: Agosto 2025
**Status**: Aceita

**Contexto**: Projeto pequeno, mapeamentos simples. MapStruct adicionaria dependência e complexidade de build desnecessárias.

**Decisão**: Classe `Mapper` manual por domínio com métodos `toEntity()`, `toResponseDTO()`, `updateEntityFromDto()`.

**Consequências**: Mais controle. Pode ser migrado para MapStruct no futuro se a complexidade crescer.

---

## ADR-009 — Angular Material para o frontend

**Data**: Agosto 2025
**Status**: Aceita

**Contexto**: Precisamos de UI responsiva (RNF03). Opções avaliadas: Angular Material, PrimeNG, TailwindCSS, Bootstrap.

**Decisão**: Angular Material — é o framework oficial do Angular, bem mantido, com componentes prontos e boa integração.

---

## ADR-010 — Flyway para migrações de banco

**Data**: Agosto 2025
**Status**: Aceita

**Contexto**: Precisamos de controle de versão do schema do banco.

**Decisão**: Flyway com migrations em `src/main/resources/db/migration/`. Nunca editar migration já aplicada.
