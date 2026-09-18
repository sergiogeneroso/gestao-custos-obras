import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { LogAuditoriaResponseDTO, OPERACAO_AUDITORIA_LABEL } from '../auditoria.model';
import { AuditoriaService } from '../auditoria.service';

export interface HistoricoDialogData {
  entidade: string;
  entidadeId: number;
  /** Ex. "Despesa #42" — título do diálogo. */
  titulo: string;
}

// Diálogo genérico e compartilhado (ADR-042): toda tela "ver histórico" de qualquer domínio abre
// este mesmo componente, em vez de cada domínio ganhar o seu.
@Component({
  selector: 'app-historico-dialog',
  imports: [DatePipe, MatButtonModule, MatDialogModule],
  templateUrl: './historico-dialog.html',
  styleUrl: './historico-dialog.scss',
})
export class HistoricoDialog {
  private readonly service = inject(AuditoriaService);
  private readonly dialogRef = inject(MatDialogRef<HistoricoDialog>);
  protected readonly data = inject<HistoricoDialogData>(MAT_DIALOG_DATA);

  protected readonly operacaoLabel = OPERACAO_AUDITORIA_LABEL;
  protected readonly carregando = signal(true);
  protected readonly eventos = signal<LogAuditoriaResponseDTO[]>([]);
  protected readonly expandidoId = signal<number | null>(null);

  constructor() {
    this.service.consultar(this.data.entidade, this.data.entidadeId).subscribe((eventos) => {
      this.eventos.set(eventos);
      this.carregando.set(false);
    });
  }

  protected alternarExpandido(evento: LogAuditoriaResponseDTO): void {
    this.expandidoId.update((atual) => (atual === evento.id ? null : evento.id));
  }

  protected formatarEstado(json: string | null): string {
    if (!json) {
      return '—';
    }
    return JSON.stringify(JSON.parse(json), null, 2);
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
