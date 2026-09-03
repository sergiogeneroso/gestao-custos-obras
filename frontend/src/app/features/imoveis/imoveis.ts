import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { AuthImgDirective } from '../../shared/auth-img/auth-img.directive';
import { ListagemPaginada } from '../../shared/pagina/listagem-paginada';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { ImovelDetalheDialog } from './imovel-detalhe-dialog/imovel-detalhe-dialog';
import { ContratoFormDialog } from '../contratos/contrato-form-dialog/contrato-form-dialog';
import { ImovelFormDialog, ImovelFormResultado } from './imovel-form-dialog/imovel-form-dialog';
import {
  FASE_IMOVEL_LABEL,
  FaseImovel,
  ImovelResponseDTO,
  SITUACAO_IMOVEL_LABEL,
  SituacaoImovel,
} from './imovel.model';
import { ImoveisService } from './imoveis.service';

@Component({
  selector: 'app-imoveis',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatPaginatorModule,
    MatSelectModule,
    AuthImgDirective,
    BuscaToolbar,
  ],
  templateUrl: './imoveis.html',
  styleUrl: './imoveis.scss',
})
export class Imoveis {
  private readonly service = inject(ImoveisService);
  private readonly dialog = inject(MatDialog);

  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly situacaoLabel = SITUACAO_IMOVEL_LABEL;

  protected readonly busca = signal('');
  protected readonly faseFiltro = signal<FaseImovel | ''>('');
  protected readonly situacaoFiltro = signal<SituacaoImovel | ''>('');
  protected readonly layout = signal<'cards' | 'lista'>('cards');

  protected readonly faseOpcoes = Object.entries(FASE_IMOVEL_LABEL) as [FaseImovel, string][];
  protected readonly situacaoOpcoes = Object.entries(SITUACAO_IMOVEL_LABEL) as [SituacaoImovel, string][];

  // Busca e filtros são resolvidos no backend junto com a paginação: filtrar só a página
  // carregada esconderia imóveis que casam com os critérios mas estão em outra página.
  protected readonly lista = new ListagemPaginada<ImovelResponseDTO>(
    inject(DestroyRef),
    (pagina, tamanho) =>
      this.service.listarPagina(this.busca().trim(), this.faseFiltro(), this.situacaoFiltro(), pagina, tamanho),
  );

  constructor() {
    effect(() => {
      this.busca();
      this.faseFiltro();
      this.situacaoFiltro();
      this.lista.reiniciar();
    });
  }

  protected mudarPagina(evento: PageEvent): void {
    this.lista.mudarPagina(evento);
  }

  protected novo(): void {
    this.abrirFormulario(null);
  }

  protected editar(imovel: ImovelResponseDTO): void {
    this.abrirFormulario(imovel);
  }

  protected verDetalhe(imovel: ImovelResponseDTO): void {
    this.dialog
      .open(ImovelDetalheDialog, { data: { imovel }, autoFocus: false, width: '1100px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.lista.carregar());
  }

  protected inativar(imovel: ImovelResponseDTO): void {
    if (!confirm(`Inativar o imóvel "${imovel.identificador}"?`)) {
      return;
    }
    this.service.inativar(imovel.id).subscribe(() => this.lista.carregar());
  }

  private abrirFormulario(imovel: ImovelResponseDTO | null): void {
    this.dialog
      .open(ImovelFormDialog, { data: { imovel }, autoFocus: false, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((resultado?: ImovelFormResultado) => {
        this.lista.carregar();
        if (resultado?.contratoCompra) {
          this.abrirContratoDaCompra(resultado.contratoCompra);
        }
      });
  }

  /**
   * Compra parcelada: o contrato é o que define o preço do lote (ADR-037), então ele é pedido
   * logo depois do cadastro. Abrir daqui, e não de dentro do formulário do imóvel, é o que
   * garante que o overlay anterior já saiu — os dois juntos empilhavam backdrop e deixavam o
   * modal ilegível.
   */
  private abrirContratoDaCompra(contratoCompra: ImovelFormResultado['contratoCompra']): void {
    this.dialog
      .open(ContratoFormDialog, {
        data: {
          contrato: null,
          tipo: 'PARCELAMENTO_COMPRA' as const,
          ...contratoCompra,
          aviso:
            'Imóvel salvo. Como a compra é parcelada, cadastre agora o parcelamento — é ele que ' +
            'define o preço do lote.',
        },
        autoFocus: false,
        width: '680px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe(() => this.lista.carregar());
  }
}
