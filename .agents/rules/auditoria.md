---
paths:
  - "backend/src/main/java/**/despesa/**"
  - "backend/src/main/java/**/pessoa/**"
  - "backend/src/main/java/**/imovel/**"
  - "backend/src/main/java/**/contratoFinanceiro/**"
  - "backend/src/main/java/**/categoriaDespesa/**"
  - "backend/src/main/java/**/orcamentoCategoria/**"
  - "backend/src/main/java/**/shared/auditoria/**"
---

# Log de Auditoria (ADR-042)

Todo domínio de negócio nasce com auditoria — não é opcional nem algo a
perguntar ao usuário, como a skill `gerar-crud-dominio` já embute.

## A regra

Toda mutação do agregado principal de um domínio (`criar`, `atualizar`,
`excluir`, e qualquer outro método de Service que grave um novo estado nele —
`avancarFase`/`alterarSituacao` em `Imovel`, `quitar`/`pagarParcela` em
`ContratoFinanceiro`) chama `AuditoriaService.registrar(entidade,
entidadeId, operacao, estadoAnterior, estadoNovo)`, injetando
`shared/auditoria/AuditoriaService` — mesmo padrão de
`UsuarioAutenticadoService`/`ExclusaoLogica`.

- **`entidade`** é o nome da classe sem sufixo (`"Despesa"`, `"Imovel"`,
  `"ContratoFinanceiro"`), igual ao valor que o front manda como query param
  em `GET /api/auditoria`.
- **`estadoAnterior`/`estadoNovo` são o `ResponseDTO` do domínio, nunca a
  entidade JPA.** Serializar a entidade quebra em relacionamento
  bidirecional (referência circular) ou `LazyInitializationException`; o
  `ResponseDTO` já é o formato plano que a API expõe.
- **`estadoAnterior` precisa ser capturado ANTES de mutar a entidade em
  memória.** Buscar a entidade, mapear para DTO, só então aplicar os
  setters/mapper de atualização — na ordem errada, `estadoAnterior` e
  `estadoNovo` saem idênticos.
- Em `CRIACAO`, `estadoAnterior` é `null`. Em exclusão física (só
  `CategoriaDespesa`, ver `regras-negocio-financeiras.md`), `estadoNovo` é
  `null`.

## O que fica fora

- **Sub-recursos auxiliares não são auditados**: fotos, documentos do
  imóvel, anexos de despesa, documentos de contrato. São arquivo, sem
  resultado financeiro a proteger (mesma fronteira que já vale para
  exclusão lógica).
- **Exclusão em cascata do imóvel gera um único evento**, o do próprio
  `Imovel` (em `ImovelExclusaoService`). Despesas, contratos e orçamento
  cascateados **não** geram evento de auditoria próprio — continuam
  identificáveis pelos campos de `ExclusaoLogica` deles
  (`excluidoPor`/`excluidoEm`/`motivoExclusao`). Por isso o registro de
  auditoria da exclusão avulsa de contrato
  (`ContratoFinanceiroService.excluir`) fica no método `excluir`, não em
  `cascatearExclusao` — a cascata do imóvel chama `cascatearExclusao`
  diretamente, sem passar pelo `excluir` avulso.
- **Login/logout não são auditados aqui** — são evento de segurança
  diferente (sem `entidadeId`, sem estado antes/depois), fora de escopo
  desta primeira versão (ver ADR-042).

## Consulta

Endpoint único e genérico, `GET /api/auditoria?entidade=X&entidadeId=Y`
(`shared/auditoria/AuditoriaController`) — **nenhum domínio ganha rota
própria de histórico**. Acessível a qualquer usuário autenticado (RBAC
ainda não existe, ver `seguranca.md`).

## Mecanismo (por que não Envers/AOP)

Descartado Hibernate Envers (tabela `_AUD` por entidade, configuração extra
pra capturar quem fez) e AOP (camada implícita, quem lê o `Service` não vê
a auditoria acontecer) — ver ADR-042 para o raciocínio completo.
