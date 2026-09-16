import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { ConfirmExclusaoDialog } from '../../shared/confirm-exclusao-dialog/confirm-exclusao-dialog';
import { ListagemPaginada } from '../../shared/pagina/listagem-paginada';
import { DespesaDetalheDialog } from './despesa-detalhe-dialog/despesa-detalhe-dialog';
import { DespesaFormDialog } from './despesa-form-dialog/despesa-form-dialog';
import { DespesaResponseDTO } from './despesa.model';
import { DespesasService } from './despesas.service';

@Component({
  selector: 'app-despesas',
  imports: [
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatButtonToggleModule,
    MatPaginatorModule,
    BuscaToolbar,
  ],
  templateUrl: './despesas.html',
  styleUrl: './despesas.scss',
})
export class Despesas {
  private readonly service = inject(DespesasService);
  private readonly dialog = inject(MatDialog);

  protected readonly busca = signal('');
  protected readonly filtro = signal<'todas' | 'imovel' | 'geral'>('todas');

  // Busca e filtro são resolvidos no backend junto com a paginação: filtrar só a página
  // carregada esconderia lançamentos que casam com o termo mas estão em outra página.
  protected readonly lista = new ListagemPaginada<DespesaResponseDTO>(
    inject(DestroyRef),
    (pagina, tamanho) =>
      this.service.listarPagina(this.busca().trim(), this.escopo(), pagina, tamanho),
  );

  constructor() {
    effect(() => {
      this.busca();
      this.filtro();
      this.lista.reiniciar();
    });
  }

  private escopo(): 'TODAS' | 'IMOVEL' | 'GERAL' {
    return this.filtro() === 'imovel' ? 'IMOVEL' : this.filtro() === 'geral' ? 'GERAL' : 'TODAS';
  }

  protected mudarPagina(evento: PageEvent): void {
    this.lista.mudarPagina(evento);
  }

  protected novo(): void {
    this.abrirFormulario(null);
  }

  protected editar(despesa: DespesaResponseDTO): void {
    this.abrirFormulario(despesa);
  }

  // Clique na linha abre a consulta; a edição sai de dentro dela ou do botão da coluna de ações.
  protected abrirDetalhe(despesa: DespesaResponseDTO): void {
    this.dialog
      .open(DespesaDetalheDialog, {
        data: {
          despesas: this.lista.itens(),
          indice: Math.max(0, this.lista.itens().indexOf(despesa)),
        },
        autoFocus: false,
        width: '900px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe(() => this.lista.carregar());
  }

  protected excluir(despesa: DespesaResponseDTO): void {
    this.dialog
      .open(ConfirmExclusaoDialog, {
        data: { titulo: `Excluir esta despesa de ${despesa.categoriaDespesaNome}?` },
        autoFocus: false,
        width: '480px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((motivo?: string) => {
        if (!motivo) {
          return;
        }
        this.service.excluir(despesa.id, motivo).subscribe(() => this.lista.carregar());
      });
  }

  private abrirFormulario(despesa: DespesaResponseDTO | null): void {
    this.dialog
      .open(DespesaFormDialog, {
        data: { despesa },
        autoFocus: false,
        width: '640px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe(() => this.lista.carregar());
  }
}
