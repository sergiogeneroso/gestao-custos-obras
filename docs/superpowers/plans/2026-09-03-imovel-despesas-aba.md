# Aba "Despesas" do Imóvel — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir a sub-aba "Últimas despesas" do diálogo de detalhe do imóvel por uma aba "Despesas" mestre-detalhe, que lista todas as despesas do imóvel e mostra, para a despesa selecionada, seus campos e anexos (com preview inline de imagem/PDF), navegação anterior/próxima, e as ações de lançar, editar, anexar novo comprovante e remover anexo.

**Architecture:** Um novo componente standalone `ImovelDespesasAba`, aninhado em `imovel-detalhe-dialog/imovel-despesas-aba/` (é uma visão específica desse diálogo, não um domínio novo), substitui o conteúdo da aba "Últimas despesas" em `imovel-detalhe-dialog.html`. O componente é autossuficiente: busca suas próprias despesas e anexos via `DespesasService` (já existente, nenhum endpoint novo) e abre `DespesaFormDialog` (já existente) diretamente para lançar/editar. Ele emite `despesaAlterada` para o diálogo-pai atualizar o card de custo da aba "Financeiro › Dados gerais", que é a única coisa no pai que ainda depende de despesa.

**Tech Stack:** Angular 22 standalone components, signals (`signal`/`computed`/`input`/`output`), Angular Material (`MatButtonModule`, `MatFormFieldModule`, `MatSelectModule`), RxJS (`forkJoin`), `DomSanitizer` para o preview de PDF em `<iframe>`. Nenhuma dependência nova.

**Spec:** resumo fechado na sessão de grilling anterior nesta conversa (não há arquivo de spec separado — o histórico da conversa é a fonte).

## Global Constraints

- Todo código em português brasileiro (nomes de classes, variáveis, métodos, seletor do componente)
- Package by feature: o componente novo fica dentro de `features/imoveis/imovel-detalhe-dialog/`, não em `features/despesas/` nem em `shared/`
- **Nenhuma mudança de backend.** `DespesaModel`, `DespesaAnexoModel`, `GET /api/despesas?imovelId=`, `GET/POST/DELETE /api/despesas/{id}/anexos*` já cobrem tudo — não criar endpoint, DTO nem migration novos
- **Nenhuma dependência nova** — só o que já está em `frontend/package.json`
- Sem paginação nem filtros extras na aba (todas as despesas do imóvel de uma vez, ordenadas por data mais recente primeiro)
- Sem persistir status de "conferida" — a conferência é só visual
- **"Excluir despesa" não aparece nessa aba** — só na tela genérica de Despesas
- Estilo de componente: `input()`/`output()` funcionais (não `@Input()`/`@Output()`), signals para todo estado local, classes CSS com prefixo BEM do componente (`.despesas-aba__*`), ícones via Phosphor (`<i class="ph ph-...">`, nunca `mat-icon`), botão de ação primária sempre `mat-stroked-button`

---

## File Structure

**Criar:**
- `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-despesas-aba/imovel-despesas-aba.ts`
- `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-despesas-aba/imovel-despesas-aba.html`
- `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-despesas-aba/imovel-despesas-aba.scss`
- `frontend/src/app/features/despesas/despesa.model.spec.ts`

**Modificar:**
- `frontend/src/app/features/despesas/despesa.model.ts` — adiciona `tipoArquivoAnexo`
- `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-detalhe-dialog.ts` — remove o que vira responsabilidade do componente novo, importa e escuta o componente novo
- `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-detalhe-dialog.html` — troca o conteúdo da aba "Últimas despesas" pelo componente novo

---

### Task 1: `tipoArquivoAnexo` — deduzir formato do anexo pela extensão

**Files:**
- Modify: `frontend/src/app/features/despesas/despesa.model.ts`
- Create: `frontend/src/app/features/despesas/despesa.model.spec.ts`

**Interfaces:**
- Produces: `tipoArquivoAnexo(url: string): 'imagem' | 'pdf' | 'outro'` — usado pela Task 2 para decidir entre `<img>`, `<iframe>` (PDF) ou botão de download.

- [ ] **Step 1: Escrever o teste (vai falhar — a função ainda não existe)**

Criar `frontend/src/app/features/despesas/despesa.model.spec.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { tipoArquivoAnexo } from './despesa.model';

describe('tipoArquivoAnexo', () => {
  it('reconhece imagem pela extensão', () => {
    expect(tipoArquivoAnexo('/api/arquivos/download/despesas/1/nota.jpg')).toBe('imagem');
    expect(tipoArquivoAnexo('/api/arquivos/download/despesas/1/nota.PNG')).toBe('imagem');
  });

  it('reconhece pdf pela extensão', () => {
    expect(tipoArquivoAnexo('/api/arquivos/download/despesas/1/nota.pdf')).toBe('pdf');
  });

  it('cai em "outro" para extensão desconhecida ou ausente', () => {
    expect(tipoArquivoAnexo('/api/arquivos/download/despesas/1/nota.docx')).toBe('outro');
    expect(tipoArquivoAnexo('/api/arquivos/download/despesas/1/nota')).toBe('outro');
  });
});
```

- [ ] **Step 2: Rodar e confirmar que falha**

Run (na pasta `frontend/`): `npm test`
Expected: FAIL — `tipoArquivoAnexo` não é exportado por `despesa.model.ts`.

- [ ] **Step 3: Implementar**

No final de `frontend/src/app/features/despesas/despesa.model.ts`, depois da interface `DespesaAnexoResponseDTO`, adicionar:

```ts
const EXTENSOES_IMAGEM = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg']);

/** Deduz o formato do anexo pela extensão da URL, pra decidir como fazer a pré-visualização. */
export function tipoArquivoAnexo(url: string): 'imagem' | 'pdf' | 'outro' {
  const extensao = url.split('.').pop()?.toLowerCase() ?? '';
  if (extensao === 'pdf') {
    return 'pdf';
  }
  return EXTENSOES_IMAGEM.has(extensao) ? 'imagem' : 'outro';
}
```

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `npm test`
Expected: PASS — os 3 testes de `tipoArquivoAnexo` passam (mais os specs já existentes do projeto).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/despesas/despesa.model.ts frontend/src/app/features/despesas/despesa.model.spec.ts
git commit -m "$(cat <<'EOF'
feat(despesas): deduz formato do anexo pela extensão da url

Base para o preview inline de imagem/PDF na nova aba de despesas do
imóvel.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01YSjJHtsFL6gjGsoVpHuRJY
EOF
)"
```

---

### Task 2: Componente `ImovelDespesasAba` e troca da aba no diálogo do imóvel

**Files:**
- Create: `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-despesas-aba/imovel-despesas-aba.ts`
- Create: `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-despesas-aba/imovel-despesas-aba.html`
- Create: `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-despesas-aba/imovel-despesas-aba.scss`
- Modify: `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-detalhe-dialog.ts`
- Modify: `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-detalhe-dialog.html`

**Interfaces:**
- Consumes: `DespesasService` (`listar(imovelId)`, `listarAnexos(despesaId)`, `adicionarAnexo(despesaId, arquivo, tipoAnexo)`, `deletarAnexo(despesaId, anexoId)`, `baixarAnexo(url)`) de `frontend/src/app/features/despesas/despesas.service.ts` — todos já existentes, sem alteração. `DespesaFormDialog` de `frontend/src/app/features/despesas/despesa-form-dialog/despesa-form-dialog.ts`, aberto com `{ despesa, imovelId }` (já existente). `tipoArquivoAnexo` da Task 1.
- Produces: componente `ImovelDespesasAba`, seletor `app-imovel-despesas-aba`, input `imovelId = input.required<number>()`, output `despesaAlterada = output<void>()`. Consumido só por `imovel-detalhe-dialog.html`.

- [ ] **Step 1: Criar o componente**

`frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-despesas-aba/imovel-despesas-aba.ts`:

```ts
import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { mensagemErro } from '../../../../shared/erro/erro.util';
import { FASE_IMOVEL_LABEL, FaseImovel } from '../../imovel.model';
import { DespesaFormDialog } from '../../../despesas/despesa-form-dialog/despesa-form-dialog';
import {
  DespesaAnexoResponseDTO,
  DespesaResponseDTO,
  ETAPA_CONSTRUCAO_LABEL,
  TIPO_ANEXO_DESPESA_LABEL,
  TipoAnexoDespesa,
  tipoArquivoAnexo,
} from '../../../despesas/despesa.model';
import { DespesasService } from '../../../despesas/despesas.service';

interface GrupoDespesas {
  fase: FaseImovel | null;
  despesas: DespesaResponseDTO[];
}

// Fica dentro de imovel-detalhe-dialog (não em features/despesas) porque é uma leitura escopada a
// um único imóvel — lista + detalhe lado a lado, pensada pra conferir despesa e anexo, não pra
// cadastro geral, que continua na tela de Despesas.
@Component({
  selector: 'app-imovel-despesas-aba',
  imports: [CurrencyPipe, DatePipe, MatButtonModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './imovel-despesas-aba.html',
  styleUrl: './imovel-despesas-aba.scss',
})
export class ImovelDespesasAba implements OnInit, OnDestroy {
  private readonly service = inject(DespesasService);
  private readonly dialog = inject(MatDialog);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly snackBar = inject(MatSnackBar);

  readonly imovelId = input.required<number>();
  readonly despesaAlterada = output<void>();

  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly etapaLabel = ETAPA_CONSTRUCAO_LABEL;
  protected readonly tipoAnexoLabel = TIPO_ANEXO_DESPESA_LABEL;
  protected readonly tiposAnexo: TipoAnexoDespesa[] = ['COMPROVANTE', 'NOTA_FISCAL', 'RECIBO', 'CONTRATO', 'OUTRO'];
  protected readonly fases: FaseImovel[] = ['LOTE', 'CONSTRUCAO', 'CASA'];
  protected readonly formato = tipoArquivoAnexo;

  protected readonly despesas = signal<DespesaResponseDTO[]>([]);
  protected readonly selecionadaId = signal<number | null>(null);
  protected readonly anexos = signal<DespesaAnexoResponseDTO[]>([]);
  protected readonly contagemAnexos = signal<Record<number, number>>({});
  protected readonly previewImagens = signal<Record<number, string>>({});
  protected readonly previewPdfs = signal<Record<number, SafeResourceUrl>>({});
  protected readonly tipoAnexoSelecionado = signal<TipoAnexoDespesa>('COMPROVANTE');
  protected readonly enviando = signal(false);

  private objectUrls: string[] = [];

  // Mais recente primeiro. Despesa sem imóvel não aparece aqui, então faseImovel nunca é nulo na
  // prática — o filtro abaixo só satisfaz o tipo (FaseImovel | null) do DTO.
  protected readonly grupos = computed<GrupoDespesas[]>(() => {
    const ordenadas = [...this.despesas()].sort(
      (a, b) => b.dataPagamento.localeCompare(a.dataPagamento) || b.id - a.id,
    );
    const fasesPresentes = new Set(ordenadas.map((d) => d.faseImovel));
    if (fasesPresentes.size <= 1) {
      return [{ fase: null, despesas: ordenadas }];
    }
    return this.fases
      .filter((fase) => fasesPresentes.has(fase))
      .map((fase) => ({ fase, despesas: ordenadas.filter((d) => d.faseImovel === fase) }));
  });

  protected readonly listaAchatada = computed(() => this.grupos().flatMap((g) => g.despesas));
  protected readonly selecionada = computed(() => this.despesas().find((d) => d.id === this.selecionadaId()) ?? null);
  protected readonly indiceAtual = computed(() =>
    this.listaAchatada().findIndex((d) => d.id === this.selecionadaId()),
  );

  ngOnInit(): void {
    this.carregarDespesas();
  }

  ngOnDestroy(): void {
    this.objectUrls.forEach((url) => URL.revokeObjectURL(url));
  }

  protected selecionar(id: number): void {
    this.selecionadaId.set(id);
    this.carregarAnexosSelecionada(id);
  }

  protected anterior(): void {
    const indice = this.indiceAtual();
    if (indice > 0) {
      this.selecionar(this.listaAchatada()[indice - 1].id);
    }
  }

  protected proxima(): void {
    const lista = this.listaAchatada();
    const indice = this.indiceAtual();
    if (indice >= 0 && indice < lista.length - 1) {
      this.selecionar(lista[indice + 1].id);
    }
  }

  protected lancarDespesa(): void {
    this.dialog
      .open(DespesaFormDialog, {
        data: { despesa: null, imovelId: this.imovelId() },
        autoFocus: false,
        width: '680px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe(() => {
        this.carregarDespesas();
        this.despesaAlterada.emit();
      });
  }

  protected editar(despesa: DespesaResponseDTO): void {
    this.dialog
      .open(DespesaFormDialog, {
        data: { despesa, imovelId: this.imovelId() },
        autoFocus: false,
        width: '680px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe(() => {
        this.carregarDespesas();
        this.despesaAlterada.emit();
      });
  }

  protected enviarAnexo(event: Event): void {
    const elementoInput = event.target as HTMLInputElement;
    const arquivo = elementoInput.files?.[0];
    const despesaId = this.selecionadaId();
    if (!arquivo || despesaId === null) {
      return;
    }

    this.enviando.set(true);
    this.service.adicionarAnexo(despesaId, arquivo, this.tipoAnexoSelecionado()).subscribe({
      next: (anexo) => {
        this.anexos.update((atuais) => [...atuais, anexo]);
        this.contagemAnexos.update((atual) => ({ ...atual, [despesaId]: (atual[despesaId] ?? 0) + 1 }));
        this.carregarPreview(anexo);
        this.enviando.set(false);
        elementoInput.value = '';
      },
      error: (erro: HttpErrorResponse) => {
        this.enviando.set(false);
        elementoInput.value = '';
        this.snackBar.open(mensagemErro(erro, 'Não foi possível enviar o anexo.'), 'Fechar', { duration: 6000 });
      },
    });
  }

  protected removerAnexo(anexo: DespesaAnexoResponseDTO): void {
    const despesaId = this.selecionadaId();
    if (despesaId === null || !confirm(`Remover este anexo (${this.tipoAnexoLabel[anexo.tipoAnexo]})?`)) {
      return;
    }
    this.service.deletarAnexo(despesaId, anexo.id).subscribe(() => {
      this.anexos.update((atuais) => atuais.filter((a) => a.id !== anexo.id));
      this.contagemAnexos.update((atual) => ({ ...atual, [despesaId]: Math.max(0, (atual[despesaId] ?? 1) - 1) }));
    });
  }

  protected baixarAnexo(anexo: DespesaAnexoResponseDTO): void {
    this.service.baixarAnexo(anexo.url).subscribe((blob) => {
      window.open(URL.createObjectURL(blob), '_blank');
    });
  }

  private carregarDespesas(): void {
    this.service.listar(this.imovelId()).subscribe((despesas) => {
      this.despesas.set(despesas);
      this.carregarContagemAnexos(despesas);
      if (despesas.length > 0 && this.selecionadaId() === null) {
        this.selecionar(despesas[0].id);
      }
    });
  }

  // Um GET de anexos por despesa: ponytail, sem endpoint de contagem em lote no backend — se o
  // volume por imóvel crescer a ponto de doer, criar GET /api/despesas/imovel/{id}/anexos/contagem.
  private carregarContagemAnexos(despesas: DespesaResponseDTO[]): void {
    if (despesas.length === 0) {
      this.contagemAnexos.set({});
      return;
    }
    forkJoin(
      despesas.map((d) => this.service.listarAnexos(d.id).pipe(map((anexos) => [d.id, anexos.length] as const))),
    ).subscribe((pares) => this.contagemAnexos.set(Object.fromEntries(pares)));
  }

  private carregarAnexosSelecionada(despesaId: number): void {
    this.service.listarAnexos(despesaId).subscribe((anexos) => {
      this.anexos.set(anexos);
      this.contagemAnexos.update((atual) => ({ ...atual, [despesaId]: anexos.length }));
      anexos
        .filter(
          (a) =>
            this.formato(a.url) !== 'outro' &&
            !(a.id in this.previewImagens()) &&
            !(a.id in this.previewPdfs()),
        )
        .forEach((a) => this.carregarPreview(a));
    });
  }

  private carregarPreview(anexo: DespesaAnexoResponseDTO): void {
    const formato = this.formato(anexo.url);
    if (formato === 'outro') {
      return;
    }
    this.service.baixarAnexo(anexo.url).subscribe((blob) => {
      const objectUrl = URL.createObjectURL(blob);
      this.objectUrls.push(objectUrl);
      if (formato === 'imagem') {
        this.previewImagens.update((atuais) => ({ ...atuais, [anexo.id]: objectUrl }));
      } else {
        this.previewPdfs.update((atuais) => ({
          ...atuais,
          [anexo.id]: this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl),
        }));
      }
    });
  }
}
```

- [ ] **Step 2: Criar o template**

`frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-despesas-aba/imovel-despesas-aba.html`:

```html
<div class="despesas-aba">
  <div class="despesas-aba__topo">
    <button mat-stroked-button type="button" color="primary" (click)="lancarDespesa()">
      <i class="ph ph-plus"></i>
      Lançar despesa
    </button>
  </div>

  @if (despesas().length === 0) {
    <p class="despesas-aba__vazio">Nenhuma despesa lançada para este imóvel.</p>
  } @else {
    <div class="despesas-aba__corpo">
      <div class="despesas-aba__lista">
        @for (grupo of grupos(); track grupo.fase) {
          @if (grupo.fase) {
            <h4 class="despesas-aba__grupo-titulo">{{ faseLabel[grupo.fase] }}</h4>
          }
          <ul class="despesas-aba__itens">
            @for (despesa of grupo.despesas; track despesa.id) {
              <li>
                <button
                  type="button"
                  class="despesas-aba__item"
                  [class.despesas-aba__item--selecionado]="despesa.id === selecionadaId()"
                  (click)="selecionar(despesa.id)"
                >
                  <span class="despesas-aba__item-topo">
                    <span>{{ despesa.categoriaDespesaNome }}</span>
                    <span class="despesas-aba__item-valor">{{ despesa.valor | currency: 'BRL' }}</span>
                  </span>
                  <span class="despesas-aba__item-data">
                    {{ despesa.dataPagamento | date: 'dd/MM/yyyy' }}
                    @if (contagemAnexos()[despesa.id] === 0) {
                      <span class="despesas-aba__sem-anexo">
                        <i class="ph ph-warning"></i>
                        sem anexo
                      </span>
                    }
                  </span>
                </button>
              </li>
            }
          </ul>
        }
      </div>

      <div class="despesas-aba__painel">
        @if (selecionada(); as despesa) {
          <div class="despesas-aba__painel-topo">
            <div class="despesas-aba__navegacao">
              <button
                mat-icon-button
                type="button"
                [disabled]="indiceAtual() <= 0"
                (click)="anterior()"
                aria-label="Despesa anterior"
              >
                <i class="ph ph-caret-left"></i>
              </button>
              <span>{{ indiceAtual() + 1 }} de {{ listaAchatada().length }}</span>
              <button
                mat-icon-button
                type="button"
                [disabled]="indiceAtual() === listaAchatada().length - 1"
                (click)="proxima()"
                aria-label="Próxima despesa"
              >
                <i class="ph ph-caret-right"></i>
              </button>
            </div>
            <button mat-stroked-button type="button" color="primary" (click)="editar(despesa)">
              <i class="ph ph-pencil-simple"></i>
              Editar
            </button>
          </div>

          <dl class="despesas-aba__grade">
            <div>
              <dt>Valor</dt>
              <dd class="despesas-aba__valor">{{ despesa.valor | currency: 'BRL' }}</dd>
            </div>
            <div>
              <dt>Data de pagamento</dt>
              <dd>{{ despesa.dataPagamento | date: 'dd/MM/yyyy' }}</dd>
            </div>
            <div>
              <dt>Categoria</dt>
              <dd>{{ despesa.categoriaDespesaNome }}</dd>
            </div>
            <div>
              <dt>Fase</dt>
              <dd>{{ despesa.faseImovel ? faseLabel[despesa.faseImovel] : '—' }}</dd>
            </div>
            @if (despesa.etapaConstrucao) {
              <div>
                <dt>Etapa da construção</dt>
                <dd>{{ etapaLabel[despesa.etapaConstrucao] }}</dd>
              </div>
            }
            <div>
              <dt>Pagador</dt>
              <dd>{{ despesa.pagadorNome }}</dd>
            </div>
            <div>
              <dt>Beneficiário</dt>
              <dd>{{ despesa.beneficiarioNome ?? '—' }}</dd>
            </div>
            @if (despesa.descricao) {
              <div class="despesas-aba__largo">
                <dt>Descrição</dt>
                <dd>{{ despesa.descricao }}</dd>
              </div>
            }
            @if (despesa.observacao) {
              <div class="despesas-aba__largo">
                <dt>Observação</dt>
                <dd>{{ despesa.observacao }}</dd>
              </div>
            }
          </dl>

          <section class="despesas-aba__anexos">
            <h3>Anexos</h3>

            @if (anexos().length === 0) {
              <p class="despesas-aba__vazio-anexo">Nenhum anexo enviado.</p>
            } @else {
              <ul class="despesas-aba__lista-anexos">
                @for (anexo of anexos(); track anexo.id) {
                  <li>
                    <div class="despesas-aba__anexo-topo">
                      <span>
                        <i class="ph ph-paperclip"></i>
                        {{ tipoAnexoLabel[anexo.tipoAnexo] }}
                        <span class="despesas-aba__anexo-data">{{ anexo.dataUpload | date: 'dd/MM/yyyy' }}</span>
                      </span>
                      <div>
                        @if (formato(anexo.url) === 'outro') {
                          <button mat-icon-button type="button" (click)="baixarAnexo(anexo)" aria-label="Baixar anexo">
                            <i class="ph ph-download-simple"></i>
                          </button>
                        }
                        <button mat-icon-button type="button" (click)="removerAnexo(anexo)" aria-label="Remover anexo">
                          <i class="ph ph-trash"></i>
                        </button>
                      </div>
                    </div>

                    @if (formato(anexo.url) === 'imagem' && previewImagens()[anexo.id]) {
                      <img
                        class="despesas-aba__preview-imagem"
                        [src]="previewImagens()[anexo.id]"
                        [alt]="tipoAnexoLabel[anexo.tipoAnexo]"
                      />
                    } @else if (formato(anexo.url) === 'pdf' && previewPdfs()[anexo.id]) {
                      <iframe
                        class="despesas-aba__preview-pdf"
                        [src]="previewPdfs()[anexo.id]"
                        title="Pré-visualização do anexo"
                      ></iframe>
                    }
                  </li>
                }
              </ul>
            }

            <div class="despesas-aba__upload">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>Tipo do anexo</mat-label>
                <mat-select [value]="tipoAnexoSelecionado()" (selectionChange)="tipoAnexoSelecionado.set($event.value)">
                  @for (tipo of tiposAnexo; track tipo) {
                    <mat-option [value]="tipo">{{ tipoAnexoLabel[tipo] }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>

              <label class="despesas-aba__upload-botao">
                <i class="ph ph-upload-simple"></i>
                {{ enviando() ? 'Enviando...' : 'Anexar comprovante' }}
                <input type="file" hidden [disabled]="enviando()" (change)="enviarAnexo($event)" />
              </label>
            </div>
          </section>
        }
      </div>
    </div>
  }
</div>
```

- [ ] **Step 3: Criar o estilo**

`frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-despesas-aba/imovel-despesas-aba.scss`:

```scss
.despesas-aba__topo {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.despesas-aba__vazio {
  color: #9397ab;
  font-size: 13.5px;
}

.despesas-aba__corpo {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  align-items: start;
}

.despesas-aba__lista {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 520px;
  overflow-y: auto;
}

.despesas-aba__grupo-titulo {
  font-size: 11.5px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #9397ab;
  margin: 12px 0 4px;

  &:first-child {
    margin-top: 0;
  }
}

.despesas-aba__itens {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.despesas-aba__item {
  width: 100%;
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--mat-sys-corner-medium, 8px);
  padding: 8px 10px;
  cursor: pointer;
  color: inherit;
  font: inherit;
  display: flex;
  flex-direction: column;
  gap: 2px;

  &:hover {
    background: var(--mat-sys-surface-container-low);
  }

  &--selecionado {
    background: var(--mat-sys-surface-container);
    border-color: rgba(233, 233, 237, 0.24);
  }
}

.despesas-aba__item-topo {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
}

.despesas-aba__item-valor {
  font-weight: 500;
}

.despesas-aba__item-data {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9397ab;
}

.despesas-aba__sem-anexo {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #ff9d9d;
}

.despesas-aba__painel {
  border-left: 1px solid rgba(233, 233, 237, 0.16);
  padding-left: 20px;
}

.despesas-aba__painel-topo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.despesas-aba__navegacao {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #9397ab;
}

.despesas-aba__grade {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin: 0 0 16px;

  dt {
    font-size: 12px;
    color: #9397ab;
    margin-bottom: 2px;
  }

  dd {
    margin: 0;
  }
}

.despesas-aba__largo {
  grid-column: 1 / -1;
}

.despesas-aba__valor {
  font-size: 18px;
  font-weight: 600;
}

.despesas-aba__anexos h3 {
  margin-bottom: 8px;
}

.despesas-aba__vazio-anexo {
  color: #9397ab;
  font-size: 13.5px;
}

.despesas-aba__lista-anexos {
  list-style: none;
  padding: 0;
  margin: 0 0 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.despesas-aba__anexo-topo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13.5px;
}

.despesas-aba__anexo-data {
  display: block;
  color: #9397ab;
  font-size: 12px;
}

.despesas-aba__preview-imagem {
  max-width: 100%;
  max-height: 320px;
  border-radius: var(--mat-sys-corner-medium, 8px);
  margin-top: 6px;
  object-fit: contain;
}

.despesas-aba__preview-pdf {
  width: 100%;
  height: 420px;
  border: 1px solid rgba(233, 233, 237, 0.16);
  border-radius: var(--mat-sys-corner-medium, 8px);
  margin-top: 6px;
}

.despesas-aba__upload {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.despesas-aba__upload-botao {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px dashed rgba(233, 233, 237, 0.32);
  border-radius: 8px;
  padding: 9px 14px;
  font-size: 13.5px;
  cursor: pointer;
}
```

- [ ] **Step 4: Remover de `imovel-detalhe-dialog.ts` o que passa a ser responsabilidade do componente novo**

Em `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-detalhe-dialog.ts`:

Trocar os imports (linhas 6-9, removendo os três de despesa e acrescentando o componente novo):

```ts
import { SITUACAO_CONTRATO_LABEL, TIPO_CONTRATO_LABEL } from '../../contratos/contrato.model';
import { DespesaResponseDTO } from '../../despesas/despesa.model';
import { DespesaFormDialog } from '../../despesas/despesa-form-dialog/despesa-form-dialog';
import { DespesasService } from '../../despesas/despesas.service';
```

por:

```ts
import { SITUACAO_CONTRATO_LABEL, TIPO_CONTRATO_LABEL } from '../../contratos/contrato.model';
import { ImovelDespesasAba } from './imovel-despesas-aba/imovel-despesas-aba';
```

No decorator `@Component`, trocar o array `imports` de:

```ts
  imports: [CurrencyPipe, DatePipe, DecimalPipe, MatButtonModule, MatDialogModule, MatTabsModule],
```

por:

```ts
  imports: [CurrencyPipe, DatePipe, DecimalPipe, ImovelDespesasAba, MatButtonModule, MatDialogModule, MatTabsModule],
```

Remover a injeção do service (linha `private readonly despesasService = inject(DespesasService);`).

Remover a linha `protected readonly despesas = signal<DespesaResponseDTO[]>([]);` e o computed:

```ts
  // As mais recentes primeiro; a lista completa continua na tela de despesas.
  protected readonly despesasRecentes = computed(() =>
    [...this.despesas()].sort((a, b) => b.dataPagamento.localeCompare(a.dataPagamento)).slice(0, 5),
  );
```

Em `carregarFinanceiro()`, remover a segunda linha (a busca de despesas):

```ts
  private carregarFinanceiro(): void {
    this.relatoriosService.resultado(this.imovel().id).subscribe((resultado) => this.resultado.set(resultado));
    this.despesasService.listar(this.imovel().id).subscribe((despesas) => this.despesas.set(despesas));
  }
```

vira (também troca `private` por `protected`: depois desta task o método passa a ser chamado
direto do template, via `(despesaAlterada)="carregarFinanceiro()"` no Step 5 — todo outro método
chamado do template neste arquivo, como `anterior()` e `avancarFase()`, já é `protected`, nunca
`private`):

```ts
  protected carregarFinanceiro(): void {
    this.relatoriosService.resultado(this.imovel().id).subscribe((resultado) => this.resultado.set(resultado));
  }
```

Remover o método inteiro `lancarDespesa()`:

```ts
  protected lancarDespesa(): void {
    this.dialog
      .open(DespesaFormDialog, {
        data: { despesa: null, imovelId: this.imovel().id },
        autoFocus: false,
        width: '680px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe(() => this.carregarFinanceiro());
  }
```

- [ ] **Step 5: Trocar a aba no template**

Em `frontend/src/app/features/imoveis/imovel-detalhe-dialog/imovel-detalhe-dialog.html`, trocar o bloco (linhas 473-508):

```html
          <mat-tab label="Últimas despesas">
            <div class="detalhe-imovel__despesas-topo">
              <button mat-stroked-button type="button" color="primary" (click)="lancarDespesa()">
                <i class="ph ph-plus"></i>
                Lançar despesa
              </button>
            </div>

            @if (despesasRecentes().length === 0) {
              <p class="detalhe-imovel__vazio">Nenhuma despesa lançada para este imóvel.</p>
            } @else {
              <table class="detalhe-imovel__tabela">
                <tbody>
                  @for (despesa of despesasRecentes(); track despesa.id) {
                    <tr>
                      <td>
                        {{ despesa.categoriaDespesaNome }}
                        <span class="detalhe-imovel__complemento">
                          {{ despesa.dataPagamento | date: 'dd/MM/yyyy' }}
                          @if (despesa.faseImovel) {
                            · {{ faseLabel[despesa.faseImovel] }}
                          }
                        </span>
                      </td>
                      <td class="detalhe-imovel__num">{{ despesa.valor | currency: 'BRL' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
              @if (despesas().length > despesasRecentes().length) {
                <p class="detalhe-imovel__nota">
                  Mostrando as {{ despesasRecentes().length }} mais recentes de {{ despesas().length }}.
                </p>
              }
            }
          </mat-tab>
```

por:

```html
          <mat-tab label="Despesas">
            <app-imovel-despesas-aba [imovelId]="imovel().id" (despesaAlterada)="carregarFinanceiro()" />
          </mat-tab>
```

- [ ] **Step 6: Verificar manualmente no navegador**

Run (na pasta `frontend/`, em outro terminal, junto com o backend rodando na porta 8080): `npm start`

No navegador (`http://localhost:4200`), abrir um imóvel que já tenha despesas lançadas (algumas com anexo, pelo menos uma sem anexo, e se possível despesas em mais de uma fase) e conferir:
- A aba se chama "Despesas" (não mais "Últimas despesas") e mostra a lista completa, não só as 5 mais recentes
- Se o imóvel tem despesas em mais de uma fase, a lista aparece segmentada em seções por fase; se só numa fase, lista única
- A primeira despesa da lista já vem selecionada no painel ao abrir a aba
- Clicar em outra despesa da lista atualiza o painel de detalhe
- Os botões anterior/próxima navegam pela lista e ficam desabilitados nas pontas
- Despesa sem nenhum anexo mostra o indicador "sem anexo" na lista
- Anexo de imagem e de PDF aparecem com preview inline no painel; outro formato mostra botão de download
- "Anexar comprovante" sobe um arquivo novo e ele aparece na lista de anexos (e o indicador "sem anexo" some da lista, se era o primeiro anexo daquela despesa)
- "Remover anexo" tira o anexo da lista
- "Lançar despesa" e "Editar" abrem o formulário de despesa existente, e ao salvar a lista e o card de custo da aba "Financeiro › Dados gerais" atualizam
- A tela genérica de Despesas (`/despesas` no menu) continua funcionando como antes, sem nenhuma mudança visível

Expected: todos os pontos acima se comportam como descrito, sem erro no console do navegador.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/app/features/imoveis/imovel-detalhe-dialog/
git commit -m "$(cat <<'EOF'
feat(imoveis): aba de despesas mestre-detalhe no diálogo do imóvel

Substitui a sub-aba "Últimas despesas" (só as 5 mais recentes, sem
link pro detalhe) por uma visão completa: lista de todas as despesas
do imóvel agrupada por fase quando aplicável, indicador de despesa
sem anexo, e painel de detalhe com navegação anterior/próxima,
preview inline de anexo (imagem/PDF) e as ações de lançar, editar,
anexar e remover anexo — pra facilitar a conferência de despesa com
comprovante. Nenhuma mudança de backend: usa os endpoints de despesa
e anexo que já existiam.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01YSjJHtsFL6gjGsoVpHuRJY
EOF
)"
```

---

## Self-Review

**Cobertura da spec:**
- Nova aba mestre-detalhe substituindo "Últimas despesas" → Task 2, Step 5
- Lista de todas as despesas do imóvel, sem paginação/filtro extra, ordenada por data desc → Task 2, Step 1 (`carregarDespesas`)
- Agrupamento por fase só quando há mais de uma → Task 2, Step 1 (`grupos`)
- Indicador de despesa sem anexo → Task 2, Step 1 (`contagemAnexos`) + Step 2 (template)
- Navegação anterior/próxima → Task 2, Step 1 (`anterior`/`proxima`/`indiceAtual`)
- Anexos em lista vertical com preview inline (imagem/PDF) e fallback de download → Task 1 (`tipoArquivoAnexo`) + Task 2, Step 1 (`carregarPreview`) e Step 2 (template)
- Ações lançar despesa, editar despesa, anexar novo comprovante, remover anexo → Task 2, Step 1
- Excluir despesa fora de escopo → não implementado, nenhum botão de excluir despesa no componente novo
- Tela genérica de Despesas sem alteração → nenhuma task toca `features/despesas/despesas.ts`/`.html`
- Sem mudança de backend → nenhuma task toca `backend/`

Nenhuma lacuna encontrada.

**Placeholders:** nenhum "TBD"/"similar to Task N"/passo sem código — checado.

**Consistência de tipos:** `imovelId = input.required<number>()` (Task 2) é lido como `this.imovelId()` em todos os métodos do próprio componente e alimentado por `[imovelId]="imovel().id"` no template do pai (Task 2, Step 5) — `imovel().id` é `number`, bate. `despesaAlterada = output<void>()` é emitido sem argumento em `lancarDespesa()`/`editar()` e escutado como `(despesaAlterada)="carregarFinanceiro()"`, que não espera argumento — bate. `tipoArquivoAnexo` (Task 1) devolve `'imagem' | 'pdf' | 'outro'`, e o único consumidor (`this.formato`, Task 2) trata os três casos no template e em `carregarPreview`.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-03-imovel-despesas-aba.md`. Duas opções de execução:

1. **Subagent-Driven (recomendado)** — dispato um subagente novo por task, com revisão entre elas
2. **Inline Execution** — executo as tasks nesta sessão, em lote, com checkpoints pra revisão

Qual prefere?
