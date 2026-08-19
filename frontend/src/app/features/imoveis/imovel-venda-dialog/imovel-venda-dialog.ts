import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { paraIso } from '../../../shared/data/data.util';
import { MoedaDirective } from '../../../shared/moeda/moeda.directive';
import { PessoaResponseDTO } from '../../pessoas/pessoa.model';
import { PessoasService } from '../../pessoas/pessoas.service';
import { ImovelResponseDTO } from '../imovel.model';
import { ImoveisService } from '../imoveis.service';

export interface ImovelVendaDialogData {
  imovel: ImovelResponseDTO;
}

@Component({
  selector: 'app-imovel-venda-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MoedaDirective,
  ],
  templateUrl: './imovel-venda-dialog.html',
  styleUrl: './imovel-venda-dialog.scss',
})
export class ImovelVendaDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ImoveisService);
  private readonly pessoasService = inject(PessoasService);
  private readonly dialogRef = inject(MatDialogRef<ImovelVendaDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<ImovelVendaDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = this.data.imovel;
  protected readonly pessoas = signal<PessoaResponseDTO[]>([]);
  protected readonly salvando = signal(false);

  protected readonly form = this.fb.group({
    valorVenda: [this.imovel.vendaValorPretendido ?? null, Validators.required],
    dataVenda: [new Date() as Date | null, Validators.required],
    compradorId: [null as number | null, Validators.required],
  });

  ngOnInit(): void {
    this.pessoasService.listar().subscribe((pessoas) => this.pessoas.set(pessoas.filter((p) => p.ativo)));
  }

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);
    const bruto = this.form.getRawValue();
    this.service
      .alterarSituacao(this.imovel.id, {
        novaSituacao: 'VENDIDO',
        valorVenda: bruto.valorVenda,
        dataVenda: paraIso(bruto.dataVenda),
        compradorId: bruto.compradorId,
      })
      .subscribe({
        next: (atualizado) => {
          this.snackBar.open('Venda registrada com sucesso.', 'Fechar', { duration: 4000 });
          this.dialogRef.close(atualizado);
        },
        error: (erro: HttpErrorResponse) => {
          this.salvando.set(false);
          this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível registrar a venda.', 'Fechar', {
            duration: 6000,
          });
        },
      });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
