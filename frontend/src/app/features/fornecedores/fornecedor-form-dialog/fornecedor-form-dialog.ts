import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PessoaResponseDTO } from '../../pessoas/pessoa.model';
import { PessoasService } from '../../pessoas/pessoas.service';
import { FornecedorRequestDTO, FornecedorResponseDTO } from '../fornecedor.model';
import { FornecedoresService } from '../fornecedores.service';

export interface FornecedorFormDialogData {
  fornecedor: FornecedorResponseDTO | null;
}

@Component({
  selector: 'app-fornecedor-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './fornecedor-form-dialog.html',
  styleUrl: './fornecedor-form-dialog.scss',
})
export class FornecedorFormDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(FornecedoresService);
  private readonly pessoasService = inject(PessoasService);
  private readonly dialogRef = inject(MatDialogRef<FornecedorFormDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<FornecedorFormDialogData>(MAT_DIALOG_DATA);

  protected readonly fornecedor = this.data.fornecedor;
  protected readonly pessoas = signal<PessoaResponseDTO[]>([]);
  protected readonly salvando = signal(false);

  protected readonly form = this.fb.group({
    pessoaId: [this.fornecedor?.pessoaId ?? (null as number | null), Validators.required],
    areaAtuacao: [this.fornecedor?.areaAtuacao ?? ''],
    observacoes: [this.fornecedor?.observacoes ?? ''],
  });

  ngOnInit(): void {
    this.pessoasService.listar().subscribe((pessoas) => this.pessoas.set(pessoas.filter((p) => p.ativo)));
  }

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);

    const dto = this.form.getRawValue() as FornecedorRequestDTO;
    const requisicao = this.fornecedor
      ? this.service.atualizar(this.fornecedor.id, dto)
      : this.service.criar(dto);

    requisicao.subscribe({
      next: () => {
        this.snackBar.open('Fornecedor salvo com sucesso.', 'Fechar', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        const mensagem = erro.error?.mensagem ?? 'Não foi possível salvar o fornecedor.';
        this.snackBar.open(mensagem, 'Fechar', { duration: 6000 });
      },
    });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
