import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { ListagemPaginada } from '../../shared/pagina/listagem-paginada';
import { ContratoDetalheDialog } from './contrato-detalhe-dialog/contrato-detalhe-dialog';
import { ContratoFormDialog } from './contrato-form-dialog/contrato-form-dialog';
import { ContratoFinanceiroResponseDTO, SITUACAO_CONTRATO_LABEL, TIPO_CONTRATO_LABEL } from './contrato.model';
import { ContratosService } from './contratos.service';

@Component({
  selector: 'app-contratos',
  imports: [CurrencyPipe, MatButtonModule, MatPaginatorModule, BuscaToolbar],
  templateUrl: './contratos.html',
  styleUrl: './contratos.scss',
})
export class Contratos {
  private readonly service = inject(ContratosService);
  private readonly dialog = inject(MatDialog);

  protected readonly tipoLabel = TIPO_CONTRATO_LABEL;
  protected readonly situacaoLabel = SITUACAO_CONTRATO_LABEL;

  protected readonly busca = signal('');

  // A busca é resolvida no backend junto com a paginação: filtrar só a página carregada
  // esconderia contratos que casam com o termo mas estão em outra página.
  protected readonly lista = new ListagemPaginada<ContratoFinanceiroResponseDTO>(
    inject(DestroyRef),
    (pagina, tamanho) => this.service.listarPagina(this.busca().trim(), pagina, tamanho),
  );

  constructor() {
    effect(() => {
      this.busca();
      this.lista.reiniciar();
    });
  }

  protected mudarPagina(evento: PageEvent): void {
    this.lista.mudarPagina(evento);
  }

  protected novo(): void {
    this.abrirFormulario(null);
  }

  protected editar(contrato: ContratoFinanceiroResponseDTO): void {
    this.abrirFormulario(contrato);
  }

  private abrirFormulario(contrato: ContratoFinanceiroResponseDTO | null): void {
    this.dialog
      .open(ContratoFormDialog, { data: { contrato }, autoFocus: false, width: '680px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.lista.carregar());
  }

  protected verDetalhe(contrato: ContratoFinanceiroResponseDTO): void {
    this.dialog
      .open(ContratoDetalheDialog, { data: { contrato }, autoFocus: false, width: '680px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((acao) => {
        this.lista.carregar();
        if (acao === 'editar') {
          this.abrirFormulario(contrato);
        }
      });
  }
}
