import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FASE_IMOVEL_LABEL, ImovelResponseDTO, PROXIMA_FASE } from '../imovel.model';
import { ImoveisService } from '../imoveis.service';

export interface ImovelFaseDialogData {
  imovel: ImovelResponseDTO;
}

@Component({
  selector: 'app-imovel-fase-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './imovel-fase-dialog.html',
  styleUrl: './imovel-fase-dialog.scss',
})
export class ImovelFaseDialog {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ImoveisService);
  private readonly dialogRef = inject(MatDialogRef<ImovelFaseDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<ImovelFaseDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = this.data.imovel;
  protected readonly proximaFase = PROXIMA_FASE[this.imovel.fase]!;
  protected readonly faseLabel = FASE_IMOVEL_LABEL;

  protected readonly salvando = signal(false);

  protected readonly form = this.fb.group({
    data: [new Date().toISOString().slice(0, 10), Validators.required],
  });

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);
    this.service
      .avancarFase(this.imovel.id, { novaFase: this.proximaFase, data: this.form.getRawValue().data! })
      .subscribe({
        next: (atualizado) => {
          this.snackBar.open('Fase atualizada com sucesso.', 'Fechar', { duration: 4000 });
          this.dialogRef.close(atualizado);
        },
        error: (erro: HttpErrorResponse) => {
          this.salvando.set(false);
          this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível avançar a fase.', 'Fechar', { duration: 6000 });
        },
      });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
