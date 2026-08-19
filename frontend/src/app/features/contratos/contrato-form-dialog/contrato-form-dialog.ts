import { CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { paraData, paraIso } from '../../../shared/data/data.util';
import { MoedaDirective } from '../../../shared/moeda/moeda.directive';
import { ImovelResponseDTO } from '../../imoveis/imovel.model';
import { ImoveisService } from '../../imoveis/imoveis.service';
import { PessoaResponseDTO } from '../../pessoas/pessoa.model';
import { PessoasService } from '../../pessoas/pessoas.service';
import {
  ContratoFinanceiroRequestDTO,
  ContratoFinanceiroResponseDTO,
  TIPO_CONTRATO_LABEL,
  TipoContratoFinanceiro,
} from '../contrato.model';
import { ContratosService } from '../contratos.service';

type ParcelaFormGroup = FormGroup<{
  numero: FormControl<number | null>;
  dataVencimento: FormControl<Date | null>;
  valor: FormControl<number | null>;
  valorJuros: FormControl<number | null>;
  paga: FormControl<boolean | null>;
}>;

export interface ContratoFormDialogData {
  contrato: ContratoFinanceiroResponseDTO | null;
  // Pré-preenchimento vindo do cadastro do imóvel parcelado (ADR-037).
  imovelId?: number | null;
  tipo?: TipoContratoFinanceiro | null;
  contraparteId?: number | null;
  dataCompra?: string | null;
}

@Component({
  selector: 'app-contrato-form-dialog',
  imports: [
    CurrencyPipe,
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
  protected readonly data = inject<ContratoFormDialogData>(MAT_DIALOG_DATA, { optional: true });

  protected readonly contrato = this.data?.contrato ?? null;

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
    imovelId: [this.contrato?.imovelId ?? this.data?.imovelId ?? (null as number | null), Validators.required],
    tipo: [this.contrato?.tipo ?? this.data?.tipo ?? (null as TipoContratoFinanceiro | null), Validators.required],
    contraparteId: [
      this.contrato?.contraparteId ?? this.data?.contraparteId ?? (null as number | null),
      Validators.required,
    ],
    valorContratado: [this.contrato?.valorContratado ?? (null as number | null), Validators.required],
  });

  // Só na compra do lote: a entrada e o preço à vista não fazem sentido nos outros tipos (ADR-037).
  protected readonly compra = this.fb.group({
    entradaValor: [null as number | null],
    entradaData: [paraData(this.data?.dataCompra) ?? new Date() as Date | null],
    precoAVistaLote: [null as number | null],
  });

  // Gerador de cronograma: preenche o FormArray de uma vez, e as linhas continuam editáveis depois.
  protected readonly gerador = this.fb.group({
    quantidade: [12 as number | null, [Validators.required, Validators.min(1), Validators.max(600)]],
    valorParcela: [null as number | null, Validators.required],
    primeiroVencimento: [null as Date | null, Validators.required],
  });

  protected readonly parcelas = this.fb.array<ParcelaFormGroup>([]);

  // As linhas do cronograma mudam por push/remove além de digitação, então a fonte destes computeds
  // é o valueChanges do próprio FormArray, não uma leitura pontual dos controles.
  private readonly tipoSelecionado = toSignal(this.form.controls.tipo.valueChanges, {
    initialValue: this.form.controls.tipo.value,
  });
  private readonly entradaInformada = toSignal(this.compra.controls.entradaValor.valueChanges, {
    initialValue: this.compra.controls.entradaValor.value,
  });
  private readonly precoInformado = toSignal(this.compra.controls.precoAVistaLote.valueChanges, {
    initialValue: this.compra.controls.precoAVistaLote.value,
  });
  private readonly linhasParcela = toSignal(this.parcelas.valueChanges, {
    initialValue: this.parcelas.getRawValue(),
  });

  // ---- Reconciliação preço do lote x cronograma (ADR-037) ------------------------------------
  //
  // O parcelamento do lote é normalmente SEM juros: entrada + parcelas fecham com o preço. Por isso
  // isto é uma linha que se recalcula sozinha, não um botão que o usuário precise lembrar de apertar
  // — no caminho normal ele não interage com juros em momento nenhum.

  protected readonly ehCompraDeLote = computed(() => this.tipoSelecionado() === 'PARCELAMENTO_COMPRA');

  protected readonly totalCronograma = computed(() => {
    const entrada = this.entradaInformada() ?? 0;
    return entrada + this.valoresParcelas().reduce((soma, valor) => soma + valor, 0);
  });

  /** Positivo = juros embutidos; zero = sem juros; negativo = preço informado maior que o total. */
  protected readonly diferencaJuros = computed(() => {
    const preco = this.precoInformado();
    return preco == null ? 0 : Math.round((this.totalCronograma() - preco) * 100) / 100;
  });

  protected readonly precoNaoInformado = computed(() => this.precoInformado() == null);

  protected distribuirJuros(): void {
    const total = this.diferencaJuros();
    const valores = this.valoresParcelas();
    const somaParcelas = valores.reduce((soma, valor) => soma + valor, 0);
    if (total <= 0 || somaParcelas <= 0) {
      return;
    }

    // ponytail: rateio linear, proporcional ao valor da parcela — não é tabela Price/SAC. Para
    // acompanhamento de custo basta, e a quitação antecipada tem valor próprio negociado que
    // substitui as parcelas restantes de qualquer forma.
    let alocado = 0;
    this.parcelas.controls.forEach((linha, indice) => {
      const ultima = indice === this.parcelas.length - 1;
      const juros = ultima
        ? Math.round((total - alocado) * 100) / 100 // a sobra de arredondamento fecha na última
        : Math.round((total * (valores[indice] / somaParcelas)) * 100) / 100;
      alocado += juros;
      linha.controls.valorJuros.setValue(juros);
    });
  }

  private valoresParcelas(): number[] {
    return (this.linhasParcela() ?? []).map((linha) => linha.valor ?? 0);
  }

  constructor() {
    // O valor contratado da compra do lote é entrada + parcelas. Segue o cronograma sozinho até o
    // usuário digitar algo ali — a partir daí o que ele escreveu manda.
    effect(() => {
      const total = this.totalCronograma();
      if (this.ehCompraDeLote() && this.form.controls.valorContratado.pristine && total > 0) {
        this.form.controls.valorContratado.setValue(total, { emitEvent: false });
      }
    });
  }

  ngOnInit(): void {
    this.imoveisService.listar().subscribe((imoveis) => this.imoveis.set(imoveis.filter((i) => i.ativo)));
    this.pessoasService.listar().subscribe((pessoas) => this.pessoas.set(pessoas.filter((p) => p.ativo)));

    if (this.contrato) {
      this.contrato.parcelas.forEach((parcela) =>
        this.parcelas.push(
          this.novaLinhaParcela(parcela.numero, paraData(parcela.dataVencimento), parcela.valor, parcela.valorJuros, parcela.dataPagamento !== null),
        ),
      );
    } else {
      this.adicionarParcela();
    }
  }

  private novaLinhaParcela(
    numero: number,
    dataVencimento: Date | null = null,
    valor: number | null = null,
    valorJuros: number | null = null,
    paga = false,
  ): ParcelaFormGroup {
    const linha = this.fb.group({
      numero: [numero as number | null, Validators.required],
      dataVencimento: [dataVencimento, Validators.required],
      valor: [valor, Validators.required],
      valorJuros: [valorJuros],
      paga: [paga],
    });

    // Parcela paga é histórico: o backend recusa alteração, então a tela nem oferece.
    if (paga) {
      linha.controls.numero.disable();
      linha.controls.dataVencimento.disable();
      linha.controls.valor.disable();
      linha.controls.valorJuros.disable();
    }

    return linha;
  }

  protected adicionarParcela(): void {
    this.parcelas.push(this.novaLinhaParcela(this.proximoNumero()));
  }

  protected removerParcela(indice: number): void {
    this.parcelas.removeAt(indice);
  }

  protected gerarParcelas(): void {
    if (this.gerador.invalid) {
      this.gerador.markAllAsTouched();
      return;
    }

    const { quantidade, valorParcela, primeiroVencimento } = this.gerador.getRawValue();

    // As pagas ficam: o cronograma gerado só substitui o que ainda está em aberto.
    for (let i = this.parcelas.length - 1; i >= 0; i--) {
      if (!this.parcelas.at(i).controls.paga.value) {
        this.parcelas.removeAt(i);
      }
    }

    const numeroInicial = this.proximoNumero();
    for (let i = 0; i < quantidade!; i++) {
      this.parcelas.push(
        this.novaLinhaParcela(numeroInicial + i, this.somarMeses(primeiroVencimento!, i), valorParcela),
      );
    }
  }

  // Vencimento dia 31 num mês de 30 cai para o último dia do mês, em vez de pular para o mês seguinte.
  private somarMeses(base: Date, meses: number): Date {
    const alvo = new Date(base.getFullYear(), base.getMonth() + meses, 1);
    const ultimoDia = new Date(alvo.getFullYear(), alvo.getMonth() + 1, 0).getDate();
    alvo.setDate(Math.min(base.getDate(), ultimoDia));
    return alvo;
  }

  private proximoNumero(): number {
    const numeros = this.parcelas.controls.map((linha) => linha.controls.numero.value ?? 0);
    return numeros.length ? Math.max(...numeros) + 1 : 1;
  }

  protected salvar(): void {
    if (this.form.invalid || this.parcelas.invalid) {
      return;
    }

    this.salvando.set(true);

    const bruto = this.form.getRawValue();
    const compra = this.compra.getRawValue();
    const ehLote = this.ehCompraDeLote() && !this.contrato;

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
      // A entrada e o preço do lote só existem na criação da compra parcelada: na edição a parcela
      // nº 0 já está no cronograma e o valor do imóvel já foi apurado.
      entradaValor: ehLote ? compra.entradaValor : null,
      entradaData: ehLote ? paraIso(compra.entradaData) : null,
      precoAVistaLote: ehLote ? compra.precoAVistaLote : null,
    };

    const requisicao = this.contrato ? this.service.atualizar(this.contrato.id, dto) : this.service.criar(dto);

    requisicao.subscribe({
      next: () => {
        const mensagem = this.contrato ? 'Contrato atualizado com sucesso.' : 'Contrato criado com sucesso.';
        this.snackBar.open(mensagem, 'Fechar', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível salvar o contrato.', 'Fechar', { duration: 6000 });
      },
    });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
