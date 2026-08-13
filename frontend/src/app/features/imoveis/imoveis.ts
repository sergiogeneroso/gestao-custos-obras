import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { ImovelDetalheDialog } from './imovel-detalhe-dialog/imovel-detalhe-dialog';
import { ImovelFormDialog } from './imovel-form-dialog/imovel-form-dialog';
import { ImovelResponseDTO, STATUS_IMOVEL_LABEL, TIPO_IMOVEL_LABEL } from './imovel.model';
import { ImoveisService } from './imoveis.service';

@Component({
  selector: 'app-imoveis',
  imports: [CurrencyPipe, DecimalPipe, MatButtonModule],
  templateUrl: './imoveis.html',
  styleUrl: './imoveis.scss',
})
export class Imoveis implements OnInit {
  private readonly service = inject(ImoveisService);
  private readonly dialog = inject(MatDialog);

  protected readonly imoveis = signal<ImovelResponseDTO[]>([]);
  protected readonly carregando = signal(true);
  protected readonly statusLabel = STATUS_IMOVEL_LABEL;
  protected readonly tipoLabel = TIPO_IMOVEL_LABEL;

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
    this.dialog.open(ImovelDetalheDialog, { data: { imovel }, autoFocus: false, width: '720px', maxWidth: '95vw' });
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
