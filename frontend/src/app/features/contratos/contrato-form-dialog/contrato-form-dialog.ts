import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { paraIso } from '../../../shared/data/data.util';
import { MoedaDirective } from '../../../shared/moeda/moeda.directive';
import { ImovelResponseDTO } from '../../imoveis/imovel.model';
import { ImoveisService } from '../../imoveis/imoveis.service';
import { PessoaResponseDTO } from '../../pessoas/pessoa.model';
import { PessoasService } from '../../pessoas/pessoas.service';
import { ContratoFinanceiroRequestDTO, TIPO_CONTRATO_LABEL, TipoContratoFinanceiro } from '../contrato.model';
import { ContratosService } from '../contratos.service';

type ParcelaFormGroup = FormGroup<{
  numero: FormControl<number | null>;
  dataVencimento: FormControl<Date | null>;
  valor: FormControl<number | null>;
  valorJuros: FormControl<number | null>;
}>;

@Component({
  selector: 'app-contrato-form-dialog',
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
  templateUrl: './contrato-form-dialog.html',
  styleUrl: './contrato-form-dialog.scss',
})
export class ContratoFormDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ContratosService);
  private readonly imoveisService = inject(ImoveisService);
  private readonly pessoasService = inject(PessoasService);
  private readonly dialogRef = inject(MatDialogRef<ContratoFormDialog>);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly tipos: TipoContratoFinanceiro[] = [
    'PARCELAMENTO_COMPRA',
    'FINANCIAMENTO_CONSTRUCAO',
    'PARCELAMENTO_VENDA',
  ];
  protected readonly tipoLabel = TIPO_CONTRATO_LABEL;

  protected readonly imoveis = signal<ImovelResponseDTO[]>([]);
  protected readonly pessoas = signal<PessoaResponseDTO[]>([]);
  protected readonly salvando = signal(false);

  protected readonly form = this.fb.group({
    imovelId: [null as number | null, Validators.required],
    tipo: [null as TipoContratoFinanceiro | null, Validators.required],
    contraparteId: [null as number | null, Validators.required],
    valorContratado: [null as number | null, Validators.required],
  });

  protected readonly parcelas = this.fb.array<ParcelaFormGroup>([]);

  ngOnInit(): void {
    this.imoveisService.listar().subscribe((imoveis) => this.imoveis.set(imoveis.filter((i) => i.ativo)));
    this.pessoasService.listar().subscribe((pessoas) => this.pessoas.set(pessoas.filter((p) => p.ativo)));
    this.adicionarParcela();
  }

  private novaLinhaParcela(numero: number): ParcelaFormGroup {
    return this.fb.group({
      numero: [numero, Validators.required],
      dataVencimento: [null as Date | null, Validators.required],
      valor: [null as number | null, Validators.required],
      valorJuros: [null as number | null],
    });
  }

  protected adicionarParcela(): void {
    this.parcelas.push(this.novaLinhaParcela(this.parcelas.length + 1));
  }

  protected removerParcela(indice: number): void {
    this.parcelas.removeAt(indice);
  }

  protected salvar(): void {
    if (this.form.invalid || this.parcelas.invalid) {
      return;
    }

    this.salvando.set(true);

    const bruto = this.form.getRawValue();
    const dto: ContratoFinanceiroRequestDTO = {
      imovelId: bruto.imovelId!,
      tipo: bruto.tipo!,
      contraparteId: bruto.contraparteId!,
      valorContratado: bruto.valorContratado!,
      parcelas: this.parcelas.getRawValue().map((parcela) => ({
        numero: parcela.numero!,
        dataVencimento: paraIso(parcela.dataVencimento)!,
        valor: parcela.valor!,
        valorJuros: parcela.valorJuros,
      })),
    };

    this.service.criar(dto).subscribe({
      next: () => {
        this.snackBar.open('Contrato criado com sucesso.', 'Fechar', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        const mensagem = erro.error?.mensagem ?? 'Não foi possível criar o contrato.';
        this.snackBar.open(mensagem, 'Fechar', { duration: 6000 });
      },
    });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
