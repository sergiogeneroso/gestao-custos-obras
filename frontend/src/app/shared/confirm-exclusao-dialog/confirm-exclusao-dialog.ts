import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface ConfirmExclusaoDialogData {
  titulo: string;
  /** Texto livre opcional — ex. contagem de registros que serão levados junto. */
  mensagem?: string;
}

/**
 * Diálogo compartilhado para qualquer exclusão lógica do sistema: pede o motivo (obrigatório)
 * e fecha devolvendo o texto digitado, ou `undefined` se cancelado.
 */
@Component({
  selector: 'app-confirm-exclusao-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './confirm-exclusao-dialog.html',
  styleUrl: './confirm-exclusao-dialog.scss',
})
export class ConfirmExclusaoDialog {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ConfirmExclusaoDialog, string>);
  protected readonly data = inject<ConfirmExclusaoDialogData>(MAT_DIALOG_DATA);

  protected readonly form = this.fb.group({
    motivo: ['', Validators.required],
  });

  protected confirmar(): void {
    if (this.form.invalid) {
      return;
    }
    this.dialogRef.close(this.form.getRawValue().motivo!);
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
