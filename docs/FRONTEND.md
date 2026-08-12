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

- Cores: paleta customizada no tema Material (não a paleta padrão), a
  definir/ajustar durante o setup
- Layout desktop-first, responsivo via Angular Material (grid/flex já
  cobrem breakpoints menores sem esforço extra de design mobile). RNF03 exige
  mobile-friendly especificamente na tela de lançamento de despesas — as
  demais telas de gestão priorizam desktop
- Listagem de imóveis: grid de cards com foto de capa (galeria RF06), badge
  de status colorido (Planejamento/Construção/Finalizado), identificador,
  tipo, endereço, área e valor. Mockup de referência gerado na sessão de
  planejamento do frontend.
