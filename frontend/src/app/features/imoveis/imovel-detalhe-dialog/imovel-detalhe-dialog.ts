import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FASE_IMOVEL_LABEL, ImovelFotoResponseDTO, ImovelResponseDTO, PROXIMA_FASE, SITUACAO_IMOVEL_LABEL } from '../imovel.model';
import { ImoveisService } from '../imoveis.service';
import { ImovelFaseDialog } from '../imovel-fase-dialog/imovel-fase-dialog';
import { ImovelVendaDialog } from '../imovel-venda-dialog/imovel-venda-dialog';

export interface ImovelDetalheDialogData {
  imovel: ImovelResponseDTO;
}

@Component({
  selector: 'app-imovel-detalhe-dialog',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, MatButtonModule, MatDialogModule],
  templateUrl: './imovel-detalhe-dialog.html',
  styleUrl: './imovel-detalhe-dialog.scss',
})
export class ImovelDetalheDialog implements OnInit, OnDestroy {
  private readonly service = inject(ImoveisService);
  private readonly dialog = inject(MatDialog);
  protected readonly data = inject<ImovelDetalheDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = signal(this.data.imovel);
  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly situacaoLabel = SITUACAO_IMOVEL_LABEL;
  protected readonly proximaFase = computed(() => PROXIMA_FASE[this.imovel().fase]);

  protected readonly fotos = signal<ImovelFotoResponseDTO[]>([]);
  protected readonly urlsFotos = signal<Record<number, string>>({});
  protected readonly indiceAtual = signal(0);

  protected readonly fotoAtual = computed(() => this.fotos()[this.indiceAtual()] ?? null);

  ngOnInit(): void {
    this.service.listarFotos(this.imovel().id).subscribe((fotos) => {
      this.fotos.set(fotos);
      fotos.forEach((foto) => this.carregarUrlFoto(foto));
    });
  }

  ngOnDestroy(): void {
    Object.values(this.urlsFotos()).forEach((url) => URL.revokeObjectURL(url));
  }

  protected anterior(): void {
    const total = this.fotos().length;
    this.indiceAtual.update((i) => (i - 1 + total) % total);
  }

  protected proxima(): void {
    const total = this.fotos().length;
    this.indiceAtual.update((i) => (i + 1) % total);
  }

  protected avancarFase(): void {
    this.dialog
      .open(ImovelFaseDialog, { data: { imovel: this.imovel() }, autoFocus: false, width: '420px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((atualizado: ImovelResponseDTO | undefined) => {
        if (atualizado) {
          this.imovel.set(atualizado);
        }
      });
  }

  protected colocarAVenda(): void {
    this.service
      .alterarSituacao(this.imovel().id, { novaSituacao: 'A_VENDA', valorVenda: null, dataVenda: null, compradorId: null })
      .subscribe((atualizado) => this.imovel.set(atualizado));
  }

  protected marcarComoAdquirido(): void {
    this.service
      .alterarSituacao(this.imovel().id, { novaSituacao: 'ADQUIRIDO', valorVenda: null, dataVenda: null, compradorId: null })
      .subscribe((atualizado) => this.imovel.set(atualizado));
  }

  protected registrarVenda(): void {
    this.dialog
      .open(ImovelVendaDialog, { data: { imovel: this.imovel() }, autoFocus: false, width: '420px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((atualizado: ImovelResponseDTO | undefined) => {
        if (atualizado) {
          this.imovel.set(atualizado);
        }
      });
  }

  private carregarUrlFoto(foto: ImovelFotoResponseDTO): void {
    this.service.baixarFoto(foto.url).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      this.urlsFotos.update((atuais) => ({ ...atuais, [foto.id]: url }));
    });
  }
}
