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
