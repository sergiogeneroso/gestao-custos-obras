import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { mensagemErro } from '../../../shared/erro/erro.util';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ImovelResponseDTO, SituacaoImovel } from '../imovel.model';
import { ImoveisService } from '../imoveis.service';

export interface ImovelDesfazerVendaDialogData {
  imovel: ImovelResponseDTO;
}

// ADR-043: desfazer a venda nunca fica bloqueado pelo estado do contrato de venda — o cadastro do
// imóvel só pede o motivo e o destino; a cascata no contrato (excluir ou cancelar com estorno)
// acontece sozinha no backend.
@Component({
  selector: 'app-imovel-desfazer-venda-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './imovel-desfazer-venda-dialog.html',
  styleUrl: './imovel-desfazer-venda-dialog.scss',
})
export class ImovelDesfazerVendaDialog {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ImoveisService);
  private readonly dialogRef = inject(MatDialogRef<ImovelDesfazerVendaDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<ImovelDesfazerVendaDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = this.data.imovel;
  protected readonly salvando = signal(false);

  protected readonly form = this.fb.group({
    destino: ['A_VENDA' as SituacaoImovel, Validators.required],
    motivo: ['', Validators.required],
  });

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);
    const bruto = this.form.getRawValue();
    this.service
      .alterarSituacao(this.imovel.id, {
        novaSituacao: bruto.destino!,
        valorVenda: null,
        dataVenda: null,
        compradorId: null,
        motivo: bruto.motivo,
      })
      .subscribe({
        next: (atualizado) => {
          this.snackBar.open('Venda desfeita com sucesso.', 'Fechar', { duration: 4000 });
          this.dialogRef.close(atualizado);
        },
        error: (erro: HttpErrorResponse) => {
          this.salvando.set(false);
          this.snackBar.open(mensagemErro(erro, 'Não foi possível desfazer a venda.'), 'Fechar', {
            duration: 6000,
          });
        },
      });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
