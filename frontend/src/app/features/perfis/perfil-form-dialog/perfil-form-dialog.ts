import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { mensagemErro } from '../../../shared/erro/erro.util';
import {
  ACAO_LABEL,
  ACOES,
  AcaoPermissao,
  DOMINIOS,
  DOMINIOS_SO_LEITURA,
  DOMINIO_LABEL,
  DominioSistema,
  PerfilRequestDTO,
  PerfilResponseDTO,
} from '../../../core/auth/permissoes.model';
import { alternarPermissao, matrizDePermissoes, permissoesDaMatriz } from '../matriz-permissao.util';
import { PerfisService } from '../perfis.service';

export interface PerfilFormDialogData {
  perfil: PerfilResponseDTO | null;
}

@Component({
  selector: 'app-perfil-form-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './perfil-form-dialog.html',
  styleUrl: './perfil-form-dialog.scss',
})
export class PerfilFormDialog {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(PerfisService);
  private readonly dialogRef = inject(MatDialogRef<PerfilFormDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<PerfilFormDialogData>(MAT_DIALOG_DATA);

  protected readonly perfil = this.data.perfil;
  protected readonly salvando = signal(false);

  protected readonly dominios = DOMINIOS;
  protected readonly acoes = ACOES;
  protected readonly dominioLabel = DOMINIO_LABEL;
  protected readonly acaoLabel = ACAO_LABEL;
  protected readonly dominiosSoLeitura = DOMINIOS_SO_LEITURA;

  protected readonly form = this.fb.group({
    nome: [this.perfil?.nome ?? '', Validators.required],
  });

  // Matriz domínio×ação em memória — mais simples que um FormArray 2D para um grid de checkbox.
  // Lógica pura (inicialização, alternância, conversão de volta) em matriz-permissao.util.ts.
  protected readonly matriz = signal(matrizDePermissoes(this.perfil?.permissoes ?? []));

  protected acaoAplicavel(dominio: DominioSistema, acao: AcaoPermissao): boolean {
    return acao === 'ACESSAR' || !this.dominiosSoLeitura.includes(dominio);
  }

  protected marcado(dominio: DominioSistema, acao: AcaoPermissao): boolean {
    return this.matriz()[dominio][acao];
  }

  protected alternar(dominio: DominioSistema, acao: AcaoPermissao): void {
    this.matriz.update((atual) => alternarPermissao(atual, dominio, acao));
  }

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);

    const dto: PerfilRequestDTO = {
      nome: this.form.getRawValue().nome!,
      permissoes: permissoesDaMatriz(this.matriz()),
    };
    const requisicao = this.perfil ? this.service.atualizar(this.perfil.id, dto) : this.service.criar(dto);

    requisicao.subscribe({
      next: () => {
        this.snackBar.open('Perfil salvo com sucesso.', 'Fechar', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        const mensagem = mensagemErro(erro, 'Não foi possível salvar o perfil.');
        this.snackBar.open(mensagem, 'Fechar', { duration: 6000 });
      },
    });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
