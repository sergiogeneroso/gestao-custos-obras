# Frontend

Angular, no diretório `frontend/` na raiz do repositório (monorepo — mesmo
histórico git do backend). Decisão completa: ADR-014 em `docs/DECISOES.md`.

## Stack

- **Angular** (via Angular CLI, versão estável mais recente no momento do
  setup) com **standalone components** — sem `NgModule` (padrão do Angular
  desde a v17, evita boilerplate de módulo por feature)
- **Angular Material** (ADR-009), tema customizado (paleta própria via Material
  theming, não a paleta padrão indigo/pink)
- **Sem NgRx** — estado local resolvido com `services` + `signals` do próprio
  Angular. Projeto pequeno, CRUD simples; NgRx adicionaria boilerplate sem
  benefício aqui (mesmo raciocínio do ADR-008, que dispensou o MapStruct)

## Estrutura de pastas

Mesma filosofia do backend (package by feature, ADR-001), adaptada ao Angular:

```
frontend/src/app/
  core/               # transversal: auth, interceptor JWT, guards, layout base
  features/
    imoveis/          # componentes + service do domínio Imóvel
    aportantes/
    etapas-projeto/
    despesas/
    orcamento-etapa/
    relatorios/
  shared/             # componentes de UI genéricos reaproveitados por 2+ features
```

Cada `features/<dominio>/` concentra os componentes (listagem, formulário,
detalhe) e o service HTTP daquele domínio — evita pastas `components/`,
`services/` na raiz.

## Autenticação e integração com a API

- Login chama `POST /api/auth/login`; token JWT recebido é guardado em
  `localStorage` (simplicidade de SPA solo; se o projeto ganhar exigências de
  segurança mais fortes, migrar para cookie `httpOnly` setado pelo backend)
- `HttpInterceptorFn` (interceptor funcional, padrão moderno do
  `HttpClient`) injeta `Authorization: Bearer <token>` em toda requisição
- `CanActivateFn` (`authGuard`) protege rotas que exigem sessão válida
- `environment.ts` / `environment.development.ts` define `apiUrl`; em dev,
  `proxy.conf.json` do Angular CLI encaminha `/api` para o backend Spring
  (evita CORS)

## Design

- Layout desktop-first, responsivo via Angular Material (grid/flex já
  cobrem breakpoints menores sem esforço extra de design mobile). RNF03 exige
  mobile-friendly especificamente na tela de lançamento de despesas — as
  demais telas de gestão priorizam desktop
- Listagem de imóveis: grid de cards com foto de capa (galeria RF06), badge
  de status colorido (Planejamento/Construção/Finalizado), identificador,
  tipo, endereço, área e valor.

### Tema e paleta de cores (RF07, ADR-016)

Tema configurável por um admin, valendo pra todo mundo (não é preferência
por usuário) — trocável em painel admin entre paletas pré-definidas, com
light e dark mode. Implementação via CSS custom properties (tokens do tema
Material M3) trocadas em runtime a partir da config vinda do backend, não
build separado por paleta.

Cinco paletas curadas, primária (topo/app-bar) + destaque (CTA/botões) +
neutra (texto secundário/borda):

| Paleta | Primária | Destaque | Neutra |
|---|---|---|---|
| **Nocturne** (default, dark-only) | `#161826` | `#9184d9` | `#9397ab` |
| Azul corporativo | `#1E3A5F` | `#2F6FED` | `#64748B` |
| Terracota industrial | `#7A2E10` | `#E8833A` | `#57534E` |
| Verde financeiro | `#1F6F4A` | `#4FA968` | `#52525B` |
| Grafite + âmbar | `#27272A` | `#F59E0B` | `#52525B` |

Cada paleta precisa de par light/dark (mesmas cores-base, ajuste de
luminância nas superfícies) — variantes exatas de dark mode ficam pra
implementação, seguindo os tokens de cor M3 do Angular Material. **Exceção:
Nocturne** não tem variante light desenhada (design system Nocturne,
ADR-018) — é a única paleta dark-only, e é a default do app hoje (RF07,
troca de paleta em runtime, ainda não implementado).

### Forma e densidade (ADR-018)

Raio de canto uniforme em 8px (`--mat-sys-corner-extra-small/small/medium`)
e densidade compacta (`density: -1`) são a linha de base global do tema
Material, independente da paleta de cor ativa — Material só varia cor por
paleta, não forma. Botões de ação primária são sempre outline
(`mat-stroked-button`), nunca preenchidos.

### Tipografia (ADR-017)

**Inter** no lugar da Roboto padrão do Material. Self-hosted via
`@fontsource/inter` (não Google Fonts via `<link>`) — evita dependência de
CDN externo em runtime e mantém o app funcional offline/sem terceiros.
Aplicada nos tokens de tipografia do tema Material M3 (`typography.ts` ou
equivalente), não só num `font-family` solto no CSS global.

### Ícones (ADR-017, emendado pela ADR-018)

**Phosphor** no lugar do Material Icons/Symbols padrão e no lugar do Lucide
originalmente escolhido — via `@phosphor-icons/web` (classe CSS, ex.
`<i class="ph ph-sign-out">`, sem wrapper Angular oficial) — não substitui
`mat-icon` onde componentes do Material já esperam um ícone Material
internamente (ex. `matSuffix` de alguns componentes), só o uso direto nas
telas do domínio.

### Landing page (ADR-018)

Rota pública `/` (fora do `Shell`, sem `authGuard`), página de apresentação
antes do login: hero, faixa de números, vitrine de imóveis, capacidades do
sistema e chamada de acesso levando pro `/login`. Conteúdo estático (não
chama a API — os endpoints exigem sessão JWT, e criar um endpoint público só
pra isso é fora de escopo). O app autenticado vive em `/painel` (antes era
`''`); o login redireciona pra `/painel/dashboard` após autenticar.

### Gráficos do dashboard (RF05, ADR-017)

**Chart.js direto**, sem wrapper Angular (`ng2-charts` etc.) — usado via
`ViewChild` num `<canvas>` dentro do componente de dashboard. Os três
gráficos do RF05 (Custo Total, R$/m², Orçado vs. Realizado) são
barra/linha/rosca simples, não justificam uma camada de integração Angular
adicional em cima do Chart.js.
