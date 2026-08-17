import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ImovelResponseDTO } from '../../imoveis/imovel.model';
import { ImoveisService } from '../../imoveis/imoveis.service';
import { PessoaResponseDTO } from '../../pessoas/pessoa.model';
import { PessoasService } from '../../pessoas/pessoas.service';
import { ContratoFinanceiroRequestDTO, TIPO_CONTRATO_LABEL, TipoContratoFinanceiro } from '../contrato.model';
import { ContratosService } from '../contratos.service';

type ParcelaFormGroup = FormGroup<{
  numero: FormControl<number | null>;
  dataVencimento: FormControl<string | null>;
  valor: FormControl<string | null>;
  valorJuros: FormControl<string | null>;
}>;

@Component({
  selector: 'app-contrato-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
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
    valorContratado: ['', Validators.required],
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
      dataVencimento: ['', Validators.required],
      valor: ['', Validators.required],
      valorJuros: [''],
    });
  }

  protected adicionarParcela(): void {
    this.parcelas.push(this.novaLinhaParcela(this.parcelas.length + 1));
  }

  protected removerParcela(indice: number): void {
    this.parcelas.removeAt(indice);
  }

  protected formatarCampoValor(controle: FormControl<string | null>): void {
    controle.setValue(this.formatarMoeda(this.parseMoeda(controle.value)), { emitEvent: false });
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
      valorContratado: this.parseMoeda(bruto.valorContratado)!,
      parcelas: this.parcelas.getRawValue().map((parcela) => ({
        numero: parcela.numero!,
        dataVencimento: parcela.dataVencimento!,
        valor: this.parseMoeda(parcela.valor)!,
        valorJuros: this.parseMoeda(parcela.valorJuros),
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

  private formatarMoeda(valor: number | null): string {
    return valor != null ? valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '';
  }

  private parseMoeda(texto: string | null): number | null {
    if (!texto) {
      return null;
    }
    const numero = Number(texto.replace(/\./g, '').replace(',', '.'));
    return Number.isNaN(numero) ? null : numero;
  }
}
