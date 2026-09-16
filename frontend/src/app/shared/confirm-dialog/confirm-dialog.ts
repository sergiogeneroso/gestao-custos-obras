import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

export interface ConfirmDialogData {
  titulo: string;
  mensagem?: string;
}

/**
 * Confirmação simples ("tem certeza?"), sem motivo — para ações que não são exclusão lógica de
 * um registro de negócio (ex. remover um arquivo). Fecha com `true` se confirmado, `undefined`
 * se cancelado. Para exclusão lógica (motivo obrigatório), ver `ConfirmExclusaoDialog`.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [MatButtonModule, MatDialogModule],
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.scss',
})
export class ConfirmDialog {
  private readonly dialogRef = inject(MatDialogRef<ConfirmDialog, boolean>);
  protected readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);

  protected confirmar(): void {
    this.dialogRef.close(true);
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
