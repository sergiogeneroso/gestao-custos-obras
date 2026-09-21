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
