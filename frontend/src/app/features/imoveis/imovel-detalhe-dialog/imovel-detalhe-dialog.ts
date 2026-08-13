import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import {
  ImovelFotoResponseDTO,
  ImovelResponseDTO,
  STATUS_IMOVEL_LABEL,
  TIPO_IMOVEL_LABEL,
} from '../imovel.model';
import { ImoveisService } from '../imoveis.service';

export interface ImovelDetalheDialogData {
  imovel: ImovelResponseDTO;
}

@Component({
  selector: 'app-imovel-detalhe-dialog',
  imports: [CurrencyPipe, DecimalPipe, MatButtonModule, MatDialogModule],
  templateUrl: './imovel-detalhe-dialog.html',
  styleUrl: './imovel-detalhe-dialog.scss',
})
export class ImovelDetalheDialog implements OnInit, OnDestroy {
  private readonly service = inject(ImoveisService);
  protected readonly data = inject<ImovelDetalheDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = this.data.imovel;
  protected readonly tipoLabel = TIPO_IMOVEL_LABEL;
  protected readonly statusLabel = STATUS_IMOVEL_LABEL;

  protected readonly fotos = signal<ImovelFotoResponseDTO[]>([]);
  protected readonly urlsFotos = signal<Record<number, string>>({});
  protected readonly indiceAtual = signal(0);

  protected readonly fotoAtual = computed(() => this.fotos()[this.indiceAtual()] ?? null);

  ngOnInit(): void {
    this.service.listarFotos(this.imovel.id).subscribe((fotos) => {
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

  private carregarUrlFoto(foto: ImovelFotoResponseDTO): void {
    this.service.baixarFoto(foto.url).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      this.urlsFotos.update((atuais) => ({ ...atuais, [foto.id]: url }));
    });
  }
}
