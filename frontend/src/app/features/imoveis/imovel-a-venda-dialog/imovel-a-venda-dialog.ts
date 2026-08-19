import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ImovelResponseDTO } from '../imovel.model';
import { ImoveisService } from '../imoveis.service';

export interface ImovelAVendaDialogData {
  imovel: ImovelResponseDTO;
}

// Colocar à venda é o momento em que o valor pretendido é decidido (ADR-033) — por isso a troca de
// situação passa por um formulário em vez de ser uma chamada direta.
@Component({
  selector: 'app-imovel-a-venda-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './imovel-a-venda-dialog.html',
  styleUrl: './imovel-a-venda-dialog.scss',
})
export class ImovelAVendaDialog {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ImoveisService);
  private readonly dialogRef = inject(MatDialogRef<ImovelAVendaDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<ImovelAVendaDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = this.data.imovel;
  protected readonly salvando = signal(false);

  protected readonly form = this.fb.group({
    vendaValorPretendido: [this.imovel.vendaValorPretendido ?? null],
  });

  protected salvar(): void {
    this.salvando.set(true);
    this.service
      .alterarSituacao(this.imovel.id, {
        novaSituacao: 'A_VENDA',
        valorVenda: null,
        dataVenda: null,
        compradorId: null,
        vendaValorPretendido: this.form.getRawValue().vendaValorPretendido,
      })
      .subscribe({
        next: (atualizado) => {
          this.snackBar.open('Imóvel colocado à venda.', 'Fechar', { duration: 4000 });
          this.dialogRef.close(atualizado);
        },
        error: (erro: HttpErrorResponse) => {
          this.salvando.set(false);
          this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível colocar à venda.', 'Fechar', {
            duration: 6000,
          });
        },
      });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
