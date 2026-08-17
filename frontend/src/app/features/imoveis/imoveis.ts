import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { AuthImgDirective } from '../../shared/auth-img/auth-img.directive';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { ImovelDetalheDialog } from './imovel-detalhe-dialog/imovel-detalhe-dialog';
import { ImovelFormDialog } from './imovel-form-dialog/imovel-form-dialog';
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
    MatSelectModule,
    AuthImgDirective,
    BuscaToolbar,
  ],
  templateUrl: './imoveis.html',
  styleUrl: './imoveis.scss',
})
export class Imoveis implements OnInit {
  private readonly service = inject(ImoveisService);
  private readonly dialog = inject(MatDialog);

  protected readonly imoveis = signal<ImovelResponseDTO[]>([]);
  protected readonly carregando = signal(true);
  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly situacaoLabel = SITUACAO_IMOVEL_LABEL;

  protected readonly busca = signal('');
  protected readonly faseFiltro = signal<FaseImovel | ''>('');
  protected readonly situacaoFiltro = signal<SituacaoImovel | ''>('');
  protected readonly layout = signal<'cards' | 'lista'>('cards');

  protected readonly faseOpcoes = Object.entries(FASE_IMOVEL_LABEL) as [FaseImovel, string][];
  protected readonly situacaoOpcoes = Object.entries(SITUACAO_IMOVEL_LABEL) as [SituacaoImovel, string][];

  protected readonly imoveisFiltrados = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    const fase = this.faseFiltro();
    const situacao = this.situacaoFiltro();

    return this.imoveis().filter((imovel) => {
      if (fase && imovel.fase !== fase) return false;
      if (situacao && imovel.situacao !== situacao) return false;
      if (!termo) return true;
      return [imovel.identificador, imovel.endereco]
        .filter((valor): valor is string => !!valor)
        .some((valor) => valor.toLowerCase().includes(termo));
    });
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected novo(): void {
    this.abrirFormulario(null);
  }

  protected editar(imovel: ImovelResponseDTO): void {
    this.abrirFormulario(imovel);
  }

  protected verDetalhe(imovel: ImovelResponseDTO): void {
    this.dialog
      .open(ImovelDetalheDialog, { data: { imovel }, autoFocus: false, width: '720px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.carregar());
  }

  protected inativar(imovel: ImovelResponseDTO): void {
    if (!confirm(`Inativar o imóvel "${imovel.identificador}"?`)) {
      return;
    }
    this.service.inativar(imovel.id).subscribe(() => this.carregar());
  }

  private abrirFormulario(imovel: ImovelResponseDTO | null): void {
    this.dialog
      .open(ImovelFormDialog, { data: { imovel }, autoFocus: false, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.carregar());
  }

  private carregar(): void {
    this.carregando.set(true);
    this.service.listar().subscribe((imoveis) => {
      this.imoveis.set(imoveis);
      this.carregando.set(false);
    });
  }
}
