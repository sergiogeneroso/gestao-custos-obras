import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FASE_IMOVEL_LABEL } from '../../imoveis/imovel.model';
import {
  DespesaAnexoResponseDTO,
  DespesaResponseDTO,
  ETAPA_CONSTRUCAO_LABEL,
  TIPO_ANEXO_DESPESA_LABEL,
} from '../despesa.model';
import { DespesasService } from '../despesas.service';

export interface DespesaDetalheDialogData {
  despesa: DespesaResponseDTO;
}

// Abrir uma despesa é consulta, não edição: a lista abre este diálogo e a edição fica atrás de um
// botão explícito, para não expor os campos a alteração acidental num clique de linha.
@Component({
  selector: 'app-despesa-detalhe-dialog',
  imports: [CurrencyPipe, DatePipe, MatButtonModule, MatDialogModule],
  templateUrl: './despesa-detalhe-dialog.html',
  styleUrl: './despesa-detalhe-dialog.scss',
})
export class DespesaDetalheDialog implements OnInit {
  private readonly service = inject(DespesasService);
  private readonly dialogRef = inject(MatDialogRef<DespesaDetalheDialog>);
  protected readonly data = inject<DespesaDetalheDialogData>(MAT_DIALOG_DATA);

  protected readonly despesa = this.data.despesa;
  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly etapaLabel = ETAPA_CONSTRUCAO_LABEL;
  protected readonly tipoAnexoLabel = TIPO_ANEXO_DESPESA_LABEL;

  protected readonly anexos = signal<DespesaAnexoResponseDTO[]>([]);

  ngOnInit(): void {
    this.service.listarAnexos(this.despesa.id).subscribe((anexos) => this.anexos.set(anexos));
  }

  protected abrirAnexo(anexo: DespesaAnexoResponseDTO): void {
    this.service.baixarAnexo(anexo.url).subscribe((blob) => {
      window.open(URL.createObjectURL(blob), '_blank');
    });
  }

  protected editar(): void {
    this.dialogRef.close('editar');
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
