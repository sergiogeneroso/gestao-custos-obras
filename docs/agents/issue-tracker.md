# Issue tracker: Local Markdown

Issues e specs deste repo vivem como arquivos markdown em `.scratch/`.

## Convenções

- Uma feature por diretório: `.scratch/<feature-slug>/`
- A spec é `.scratch/<feature-slug>/spec.md`
- Issues de implementação são um arquivo por ticket em
  `.scratch/<feature-slug>/issues/<NN>-<slug>.md`, numerados a partir de `01`,
  nunca um arquivo único combinado
- Comentários e histórico de conversa são anexados ao fim do arquivo sob um
  cabeçalho `## Comments`

## Quando uma skill disser "publish to the issue tracker"

Criar um novo arquivo em `.scratch/<feature-slug>/` (criando o diretório se
necessário).

## Quando uma skill disser "fetch the relevant ticket"

Ler o arquivo no caminho referenciado. O usuário normalmente vai passar o
caminho ou o número da issue diretamente.

## Operações de wayfinding

Usadas por `/wayfinder`. O **map** é um arquivo com um **child** file por
ticket.

- **Map**: `.scratch/<effort>/map.md` (corpo com Notes / Decisions-so-far / Fog).
- **Child ticket**: `.scratch/<effort>/issues/NN-<slug>.md`, numerado a partir
  de `01`, com a pergunta no corpo. Uma linha `Type:` registra o tipo do
  ticket (`research`/`prototype`/`grilling`/`task`); uma linha `Status:`
  registra `claimed`/`resolved`.
- **Blocking**: uma linha `Blocked by: NN, NN` perto do topo. Um ticket fica
  desbloqueado quando todo arquivo listado está `resolved`.
- **Frontier**: escanear `.scratch/<effort>/issues/` por arquivos abertos,
  desbloqueados e não reivindicados; o de menor número vence.
- **Claim**: setar `Status: claimed` e salvar antes de qualquer trabalho.
- **Resolve**: anexar a resposta sob um cabeçalho `## Answer`, setar
  `Status: resolved`, depois anexar um ponteiro de contexto (resumo + link)
  ao Decisions-so-far do `map.md`.
