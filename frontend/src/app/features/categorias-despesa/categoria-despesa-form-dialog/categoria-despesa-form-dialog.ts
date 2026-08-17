import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CategoriaDespesaRequestDTO, CategoriaDespesaResponseDTO } from '../categoria-despesa.model';
import { CategoriasDespesaService } from '../categorias-despesa.service';

export interface CategoriaDespesaFormDialogData {
  categoria: CategoriaDespesaResponseDTO | null;
}

@Component({
  selector: 'app-categoria-despesa-form-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './categoria-despesa-form-dialog.html',
  styleUrl: './categoria-despesa-form-dialog.scss',
})
export class CategoriaDespesaFormDialog {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(CategoriasDespesaService);
  private readonly dialogRef = inject(MatDialogRef<CategoriaDespesaFormDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<CategoriaDespesaFormDialogData>(MAT_DIALOG_DATA);

  protected readonly categoria = this.data.categoria;
  protected readonly salvando = signal(false);

  protected readonly form = this.fb.group({
    nome: [this.categoria?.nome ?? '', Validators.required],
    descricao: [this.categoria?.descricao ?? ''],
  });

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);

    const dto = this.form.getRawValue() as CategoriaDespesaRequestDTO;
    const requisicao = this.categoria
      ? this.service.atualizar(this.categoria.id, dto)
      : this.service.criar(dto);

    requisicao.subscribe({
      next: () => {
        this.snackBar.open('Categoria salva com sucesso.', 'Fechar', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        const mensagem = erro.error?.mensagem ?? 'Não foi possível salvar a categoria.';
        this.snackBar.open(mensagem, 'Fechar', { duration: 6000 });
      },
    });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
