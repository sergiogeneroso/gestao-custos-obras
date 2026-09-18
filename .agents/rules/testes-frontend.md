---
paths:
  - "frontend/src/**/*.spec.ts"
---

# Testes do frontend (Angular)

## Stack

Vitest com jsdom, rodado pelo builder do Angular (`ng test`) — **não** é
Jasmine nem Karma. Importe explicitamente de `vitest`
(`import { describe, expect, it, vi } from 'vitest'`); mock com `vi.fn()`.
O spec fica ao lado do arquivo testado, com o mesmo nome + `.spec.ts`.

## Por tipo de arquivo

### Função pura (`*.model.ts`, `shared/`)

Chamada direta, sem TestBed. É o teste mais barato: extraia lógica de tela
para função exportada quando ela tiver regra. Ver `despesa.model.spec.ts` e
`shared/mascara/caret.spec.ts`.

### Diretiva e componente de formulário

Componente `Host` no próprio spec, com template mínimo usando a diretiva e
um `FormControl`; monte com `TestBed.createComponent(Host)` numa função
`montar()` que devolve só o que os testes usam. Simule o usuário disparando
eventos DOM reais (`input`, `blur`) e verifique o valor exibido **e** o valor
gravado no controle. Ver `shared/mascara/diretivas.spec.ts`.

### Service HTTP

Só vale teste quando o service transforma dados (monta query, converte
resposta). Use `provideHttpClient()` + `provideHttpClientTesting()` e
`HttpTestingController` para conferir URL, parâmetros e o que volta. Service
que só repassa `http.get` fica sem teste.

### Componente de tela e diálogo

Teste o comportamento visível: o que aparece para um dado estado, o que
acontece ao clicar. Busque elementos por texto ou papel, não por classe CSS.
Dependências de backend entram como objeto fake com `vi.fn()` via
`{ provide: XService, useValue: fake }`; `MatDialogRef` idem. Abrir e fechar
diálogo é o Material funcionando — teste o conteúdo e o resultado devolvido
no `close(...)`.

## Convenções do projeto que afetam o teste

- Datas e moeda em pt-BR: provedores `MAT_DATE_LOCALE`, `DataPtBrAdapter` e
  `DATA_PT_BR_FORMATS`, como em `diretivas.spec.ts`
- Campo vazio em visualização detalhada mostra `—` (ver
  `convencoes-frontend.md`): o teste de detalhe verifica o travessão, não a
  ausência do rótulo
