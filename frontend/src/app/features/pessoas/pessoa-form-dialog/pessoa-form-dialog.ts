import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PessoaRequestDTO, PessoaResponseDTO, TIPO_PESSOA_LABEL, TipoPessoa } from '../pessoa.model';
import { PessoasService } from '../pessoas.service';

export interface PessoaFormDialogData {
  pessoa: PessoaResponseDTO | null;
}

@Component({
  selector: 'app-pessoa-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatSelectModule,
  ],
  templateUrl: './pessoa-form-dialog.html',
  styleUrl: './pessoa-form-dialog.scss',
})
export class PessoaFormDialog {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(PessoasService);
  private readonly dialogRef = inject(MatDialogRef<PessoaFormDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<PessoaFormDialogData>(MAT_DIALOG_DATA);

  protected readonly pessoa = this.data.pessoa;
  protected readonly tipos: TipoPessoa[] = ['FISICA', 'JURIDICA'];
  protected readonly tipoLabel = TIPO_PESSOA_LABEL;

  protected readonly salvando = signal(false);

  protected readonly form = this.fb.group({
    nome: [this.pessoa?.nome ?? '', Validators.required],
    tipoPessoa: [this.pessoa?.tipoPessoa ?? ('FISICA' as TipoPessoa), Validators.required],
    documento: [this.pessoa?.documento ?? '', Validators.required],
    email: [this.pessoa?.email ?? ''],
    telefone: [this.pessoa?.telefone ?? ''],
    fornecedor: [this.pessoa?.fornecedor ?? false],
    areaAtuacao: [this.pessoa?.areaAtuacao ?? ''],
    observacoes: [this.pessoa?.observacoes ?? ''],
  });

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);

    const bruto = this.form.getRawValue();
    // Desmarcar "é fornecedor" limpa os campos do papel, para não deixar dado órfão de um papel
    // que a pessoa não tem mais.
    const ehFornecedor = !!bruto.fornecedor;
    const dto = {
      ...bruto,
      email: bruto.email || null,
      telefone: bruto.telefone || null,
      fornecedor: ehFornecedor,
      areaAtuacao: ehFornecedor ? bruto.areaAtuacao || null : null,
      observacoes: ehFornecedor ? bruto.observacoes || null : null,
    } as PessoaRequestDTO;
    const requisicao = this.pessoa ? this.service.atualizar(this.pessoa.id, dto) : this.service.criar(dto);

    requisicao.subscribe({
      next: () => {
        this.snackBar.open('Pessoa salva com sucesso.', 'Fechar', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        const mensagem = erro.error?.mensagem ?? 'Não foi possível salvar a pessoa.';
        this.snackBar.open(mensagem, 'Fechar', { duration: 6000 });
      },
    });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
