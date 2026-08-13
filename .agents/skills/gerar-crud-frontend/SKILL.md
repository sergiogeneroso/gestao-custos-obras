---
name: gerar-crud-frontend
description: Use esta skill ao criar a tela CRUD Angular (listagem + formulário) de um domínio que já existe no backend (ex. "fornecedor", "categoria") seguindo o padrão já estabelecido no frontend (standalone components, signals, MatDialog, package by feature). Não use para o CRUD do backend (ver gerar-crud-dominio), nem para telas de relatório/dashboard que não são um CRUD simples.
---

# Gerar tela CRUD de um domínio no frontend

Use `features/imoveis/` como referência canônica — leia esses arquivos antes
de gerar a tela nova. É o domínio mais completo (tem soft delete, enum com
labels, upload de arquivo); os demais (`aportantes/`, `etapas-projeto/`,
`despesas/`) seguem o mesmo padrão de listagem + form dialog sem o upload.

## Passo a passo

1. **Pergunte ao usuário, se não estiver claro**: o domínio tem soft delete
   (ação "Inativar", como `Imovel`/`Aportante`) ou delete físico (como
   `EtapaProjeto`)? Algum campo é enum (precisa de `Record<Enum, string>`
   de labels, como `STATUS_IMOVEL_LABEL`)? Algum campo é monetário (precisa
   do padrão `matTextPrefix`/formatarMoeda abaixo)? Confirme os campos do
   `{Dominio}ResponseDTO`/`{Dominio}RequestDTO` do backend antes de criar o
   `.model.ts` — não invente campo que não existe na API.

2. **Criar o pacote** `frontend/src/app/features/{dominio}/`:
   - `{dominio}.model.ts` — `{Dominio}RequestDTO`/`{Dominio}ResponseDTO`
     (interfaces, espelhando os DTOs Java 1:1), tipos union para enums +
     `Record<Enum, string>` de labels (ver `TIPO_IMOVEL_LABEL` em
     `imovel.model.ts`)
   - `{dominio}.service.ts` — `@Injectable({ providedIn: 'root' })`,
     `HttpClient` injetado via `inject()`, `baseUrl = ${environment.apiUrl}/{dominio-plural}`,
     métodos `listar/criar/atualizar/inativar` (ou `deletar` se hard delete)
     retornando `Observable<...>`
   - `{dominio}.ts` + `.html` + `.scss` — componente de listagem, standalone,
     `signal` para a lista e para `carregando`, `MatDialog` injetado para
     abrir o form dialog, métodos `novo()/editar()/inativar()` (confirmação
     nativa `confirm(...)` antes de inativar/deletar)
   - `{dominio-singular}-form-dialog/{dominio-singular}-form-dialog.ts` +
     `.html` + `.scss` — `MatDialogRef`/`MAT_DIALOG_DATA` injetados,
     `FormBuilder` + `ReactiveFormsModule` com `Validators` nos campos
     obrigatórios, `salvar()` chamando `criar()` ou `atualizar()` conforme o
     dado recebido via `MAT_DIALOG_DATA` seja `null` ou não

3. **Aplicar as 3 convenções de formulário confirmadas para qualquer
   domínio** (ver `imovel-form-dialog.ts` como referência de código):
   - Campo numérico: `<input matInput type="number" step="0.01" min="0">`
     com `matTextPrefix`/`matTextSuffix` nativo pra moeda/unidade — nunca
     lib de máscara
   - `MatSnackBar` no sucesso ("{Dominio} salvo com sucesso.") e no erro
     usando a mensagem real do backend (`erro.error?.mensagem`), nunca texto
     genérico fixo
   - `dialogRef.close(true)` dentro do `next` da subscription do `salvar()`
     — o dialog só continua aberto no caminho de erro

4. **Registrar a rota** em `app.routes.ts`, dentro de `children` de
   `painel`, com `loadComponent` (lazy, mesmo padrão dos outros domínios):
   ```ts
   {
     path: '{dominio-plural}',
     loadComponent: () => import('./features/{dominio}/{dominio}').then((m) => m.{Dominio}),
   },
   ```

5. **Registrar o link no menu** em `core/layout/shell/shell.ts`, array
   `links`, com um ícone Phosphor condizente (`ph-...`, ver
   [phosphoricons.com](https://phosphoricons.com) para o nome da classe).

6. **Seguir o design já fechado** (`docs/FRONTEND.md`, ADR-016/017/018) sem
   reabrir decisão: tema/paleta via tokens Material M3 (nunca cor solta no
   `.scss`), botão de ação primária sempre `mat-stroked-button` (nunca
   preenchido), ícones via `<i class="ph ph-...">` (nunca Material
   Icons/Symbols, nunca Lucide), fonte já é Inter globalmente.

7. Se a tela tiver muitos registros, considerar grid de cards em vez de
   tabela apenas se o domínio tiver uma imagem/badge de destaque como
   `imoveis` — CRUDs simples (aportante, etapa) usam `MatTable` padrão, não
   copie o grid de cards sem necessidade.

## O que NUNCA fazer

- Não criar `NgModule` — só standalone components
- Não adicionar NgRx ou outra lib de state management — `service` +
  `signal` já resolve
- Não copiar o fluxo de upload de foto de `imoveis` para um domínio que não
  pediu isso — é específico do RF06, não um padrão geral de CRUD
- Não deixar o dialog aberto após save com sucesso, nem fechar no erro
- Não usar texto de erro genérico quando o backend já manda a mensagem real
  em `erro.error?.mensagem`
