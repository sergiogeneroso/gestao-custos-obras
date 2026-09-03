import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { mensagemErro } from '../../../../shared/erro/erro.util';
import { FASE_IMOVEL_LABEL, FaseImovel } from '../../imovel.model';
import { DespesaFormDialog } from '../../../despesas/despesa-form-dialog/despesa-form-dialog';
import {
  ContagemAnexos,
  DespesaPainelDetalhe,
} from '../../../despesas/despesa-painel-detalhe/despesa-painel-detalhe';
import { DespesaResponseDTO } from '../../../despesas/despesa.model';
import { DespesasService } from '../../../despesas/despesas.service';

interface GrupoDespesas {
  fase: FaseImovel | null;
  despesas: DespesaResponseDTO[];
}

// Fica dentro de imovel-detalhe-dialog (não em features/despesas) porque é uma leitura escopada a
// um único imóvel — lista + detalhe lado a lado, pensada pra conferir despesa e anexo, não pra
// cadastro geral, que continua na tela de Despesas. O painel da direita é o componente
// compartilhado com aquela tela; o que é próprio daqui é a lista mestre agrupada por fase.
@Component({
  selector: 'app-imovel-despesas-aba',
  imports: [CurrencyPipe, DatePipe, MatButtonModule, DespesaPainelDetalhe],
  templateUrl: './imovel-despesas-aba.html',
  styleUrl: './imovel-despesas-aba.scss',
})
export class ImovelDespesasAba implements OnInit {
  private readonly service = inject(DespesasService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly imovelId = input.required<number>();
  readonly despesaAlterada = output<void>();

  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly fases: FaseImovel[] = ['LOTE', 'CONSTRUCAO', 'CASA'];

  protected readonly despesas = signal<DespesaResponseDTO[]>([]);
  protected readonly selecionadaId = signal<number | null>(null);
  protected readonly contagemAnexos = signal<Record<number, number>>({});

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
  protected readonly selecionada = computed(
    () => this.despesas().find((d) => d.id === this.selecionadaId()) ?? null,
  );
  protected readonly indiceAtual = computed(() =>
    this.listaAchatada().findIndex((d) => d.id === this.selecionadaId()),
  );

  ngOnInit(): void {
    this.carregarDespesas();
  }

  protected selecionar(id: number): void {
    this.selecionadaId.set(id);
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

  protected registrarContagem(contagem: ContagemAnexos): void {
    this.contagemAnexos.update((atual) => ({
      ...atual,
      [contagem.despesaId]: contagem.quantidade,
    }));
  }

  protected lancarDespesa(): void {
    this.abrirFormulario(null);
  }

  protected editar(despesa: DespesaResponseDTO): void {
    this.abrirFormulario(despesa);
  }

  private abrirFormulario(despesa: DespesaResponseDTO | null): void {
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
      despesas.map((d) =>
        this.service.listarAnexos(d.id).pipe(map((anexos) => [d.id, anexos.length] as const)),
      ),
    ).subscribe({
      next: (pares) => this.contagemAnexos.set(Object.fromEntries(pares)),
      error: (erro: HttpErrorResponse) => {
        this.snackBar.open(
          mensagemErro(erro, 'Não foi possível carregar a contagem de anexos.'),
          'Fechar',
          {
            duration: 6000,
          },
        );
      },
    });
  }
}
