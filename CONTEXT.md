# Gestão de Custos de Obras

Sistema de acompanhamento do resultado financeiro de cada imóvel (lote →
construção → casa), do MVP descrito em `docs/DECISOES.md` (ADR-019 em
diante). Este glossário cobre também o vocabulário de controle de acesso
(RBAC) fechado em sessão de grilling (Set 2026).

## Linguagem

**Perfil**:
Conjunto nomeado de permissões que um usuário pode ter — o que no RBAC
clássico se chama "role". Cada usuário tem exatamente um perfil. A lista de
perfis e a matriz de permissões de cada um são configuráveis em runtime
(CRUD próprio, sem exigir deploy), diferente do campo `role` (String solta)
que existia antes desta modelagem.
_Avoid_: Role, Papel

**Domínio** (no contexto de autorização):
Unidade de autorização da matriz de permissões — corresponde 1:1 a um
pacote de negócio do backend (package-by-feature): imóvel, pessoa, despesa,
contratoFinanceiro, categoriaDespesa, relatório, auditoria e perfil. É uma
lista fixa (enum no código); ao contrário da matriz de permissões, a lista
de domínios em si não é administrável em runtime — domínios de negócio
raramente aparecem.

**Ação**:
Uma das quatro operações controláveis por domínio na matriz de permissões:
Acessar (leitura), Incluir (criação), Alterar (edição — inclui transições
de ciclo de vida, como fase/situação do imóvel), Deletar (exclusão lógica).
Acessar é pré-requisito das outras três: um perfil não pode ter Incluir,
Alterar ou Deletar marcados num domínio sem também ter Acessar nesse mesmo
domínio.

**Matriz de permissões**:
A relação perfil × domínio × ação que decide o que cada perfil pode fazer.
É a peça configurável do sistema — perfis e a matriz são editados via CRUD
próprio; domínios e ações continuam fixos no código.

**Contrato compartilhado** (ADR-047):
Um `PARCELAMENTO_COMPRA` ou `PARCELAMENTO_VENDA` que cobre mais de um lote de
uma vez — mesma entrada, mesmo cronograma de parcelas, pagamento indivisível
por lote. Cada lote vinculado tem sua própria **alocação** (fatia declarada à
mão do valor do contrato); os números do relatório (juros, saldo, quitação)
são proporcionais à fatia de cada lote, nunca ao contrato inteiro.
`FINANCIAMENTO_CONSTRUCAO` nunca é compartilhado — é sempre individual por
imóvel.
_Avoid_: Contrato coletivo, contrato múltiplo

**Alocação** (de um lote num contrato compartilhado):
O valor absoluto, em R$, que um lote específico responde dentro de um
contrato compartilhado — declarado à mão pelo usuário ao vincular o lote,
nunca calculado ou dividido automaticamente pelo sistema (mesmo princípio da
despesa compartilhada: sem rateio automático entre imóveis).
_Avoid_: Rateio, divisão, cota
