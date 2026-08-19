import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  ContratoDocumentoResponseDTO,
  ContratoFinanceiroResponseDTO,
  ParcelaContratoResponseDTO,
  SITUACAO_CONTRATO_LABEL,
  TIPO_CONTRATO_LABEL,
  TIPO_DOCUMENTO_CONTRATO_LABEL,
  TIPOS_DOCUMENTO_CONTRATO,
  TipoDocumentoContrato,
} from '../contrato.model';
import { paraIso } from '../../../shared/data/data.util';
import { MoedaDirective } from '../../../shared/moeda/moeda.directive';
import { ContratosService } from '../contratos.service';

export interface ContratoDetalheDialogData {
  contrato: ContratoFinanceiroResponseDTO;
}

@Component({
  selector: 'app-contrato-detalhe-dialog',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MoedaDirective,
  ],
  templateUrl: './contrato-detalhe-dialog.html',
  styleUrl: './contrato-detalhe-dialog.scss',
})
export class ContratoDetalheDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ContratosService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<ContratoDetalheDialog>);
  protected readonly data = inject<ContratoDetalheDialogData>(MAT_DIALOG_DATA);

  protected readonly contrato = signal(this.data.contrato);
  protected readonly tipoLabel = TIPO_CONTRATO_LABEL;
  protected readonly situacaoLabel = SITUACAO_CONTRATO_LABEL;

  protected readonly parcelaEmBaixa = signal<number | null>(null);
  protected readonly quitandoContrato = signal(false);
  protected readonly salvando = signal(false);

  protected readonly tiposDocumento = TIPOS_DOCUMENTO_CONTRATO;
  protected readonly tipoDocumentoLabel = TIPO_DOCUMENTO_CONTRATO_LABEL;
  protected readonly documentos = signal<ContratoDocumentoResponseDTO[]>([]);
  protected readonly tipoDocumentoSelecionado = signal<TipoDocumentoContrato>('CONTRATO');
  protected readonly enviandoDocumento = signal(false);

  ngOnInit(): void {
    this.service.listarDocumentos(this.contrato().id).subscribe((documentos) => this.documentos.set(documentos));
  }

  protected readonly formBaixa = this.fb.group({
    dataPagamento: [new Date() as Date | null, Validators.required],
    valorPago: [null as number | null, Validators.required],
  });

  protected readonly formQuitacao = this.fb.group({
    dataQuitacao: [new Date() as Date | null, Validators.required],
    valorQuitacao: [null as number | null, Validators.required],
  });

  protected iniciarBaixa(parcela: ParcelaContratoResponseDTO): void {
    this.parcelaEmBaixa.set(parcela.id);
    this.formBaixa.setValue({
      dataPagamento: new Date(),
      valorPago: parcela.valor,
    });
  }

  protected cancelarBaixa(): void {
    this.parcelaEmBaixa.set(null);
  }

  protected confirmarBaixa(parcelaId: number): void {
    if (this.formBaixa.invalid) {
      return;
    }
    this.salvando.set(true);
    const bruto = this.formBaixa.getRawValue();
    this.service
      .pagarParcela(this.contrato().id, parcelaId, {
        dataPagamento: paraIso(bruto.dataPagamento)!,
        valorPago: bruto.valorPago!,
      })
      .subscribe({
        next: (atualizado) => {
          this.contrato.set(atualizado);
          this.parcelaEmBaixa.set(null);
          this.salvando.set(false);
          this.snackBar.open('Parcela baixada com sucesso.', 'Fechar', { duration: 4000 });
        },
        error: (erro: HttpErrorResponse) => {
          this.salvando.set(false);
          this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível dar baixa na parcela.', 'Fechar', {
            duration: 6000,
          });
        },
      });
  }

  protected iniciarQuitacao(): void {
    this.quitandoContrato.set(true);
  }

  protected cancelarQuitacao(): void {
    this.quitandoContrato.set(false);
  }

  protected confirmarQuitacao(): void {
    if (this.formQuitacao.invalid) {
      return;
    }
    this.salvando.set(true);
    const bruto = this.formQuitacao.getRawValue();
    this.service
      .quitar(this.contrato().id, {
        dataQuitacao: paraIso(bruto.dataQuitacao)!,
        valorQuitacao: bruto.valorQuitacao!,
      })
      .subscribe({
        next: (atualizado) => {
          this.contrato.set(atualizado);
          this.quitandoContrato.set(false);
          this.salvando.set(false);
          this.snackBar.open('Contrato quitado com sucesso.', 'Fechar', { duration: 4000 });
        },
        error: (erro: HttpErrorResponse) => {
          this.salvando.set(false);
          this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível quitar o contrato.', 'Fechar', {
            duration: 6000,
          });
        },
      });
  }

  protected enviarDocumento(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo) {
      return;
    }

    this.enviandoDocumento.set(true);
    this.service.adicionarDocumento(this.contrato().id, arquivo, this.tipoDocumentoSelecionado(), null).subscribe({
      next: (documento) => {
        this.documentos.update((atuais) => [...atuais, documento]);
        this.enviandoDocumento.set(false);
        input.value = '';
      },
      error: (erro: HttpErrorResponse) => {
        this.enviandoDocumento.set(false);
        input.value = '';
        this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível enviar o documento.', 'Fechar', { duration: 6000 });
      },
    });
  }

  protected abrirDocumento(documento: ContratoDocumentoResponseDTO): void {
    this.service.baixarDocumento(documento.url).subscribe((blob) => {
      window.open(URL.createObjectURL(blob), '_blank');
    });
  }

  protected removerDocumento(documento: ContratoDocumentoResponseDTO): void {
    if (!confirm(`Remover o documento "${documento.nomeArquivo ?? documento.id}"?`)) {
      return;
    }
    this.service.deletarDocumento(this.contrato().id, documento.id).subscribe(() => {
      this.documentos.update((atuais) => atuais.filter((d) => d.id !== documento.id));
    });
  }

  protected editar(): void {
    this.dialogRef.close('editar');
  }

}
