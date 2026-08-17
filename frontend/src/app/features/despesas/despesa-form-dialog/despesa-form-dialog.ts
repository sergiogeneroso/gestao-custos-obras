import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CategoriaDespesaResponseDTO } from '../../categorias-despesa/categoria-despesa.model';
import { CategoriasDespesaService } from '../../categorias-despesa/categorias-despesa.service';
import { ContratoFinanceiroResponseDTO, TIPO_CONTRATO_LABEL } from '../../contratos/contrato.model';
import { ContratosService } from '../../contratos/contratos.service';
import { FASE_IMOVEL_LABEL, FaseImovel, ImovelResponseDTO } from '../../imoveis/imovel.model';
import { ImoveisService } from '../../imoveis/imoveis.service';
import { PessoaResponseDTO } from '../../pessoas/pessoa.model';
import { PessoasService } from '../../pessoas/pessoas.service';
import { DespesaAnexoResponseDTO, DespesaRequestDTO, DespesaResponseDTO, TIPO_ANEXO_DESPESA_LABEL, TipoAnexoDespesa } from '../despesa.model';
import { DespesasService } from '../despesas.service';

export interface DespesaFormDialogData {
  despesa: DespesaResponseDTO | null;
}

@Component({
  selector: 'app-despesa-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './despesa-form-dialog.html',
  styleUrl: './despesa-form-dialog.scss',
})
export class DespesaFormDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(DespesasService);
  private readonly imoveisService = inject(ImoveisService);
  private readonly categoriasService = inject(CategoriasDespesaService);
  private readonly pessoasService = inject(PessoasService);
  private readonly contratosService = inject(ContratosService);
  private readonly dialogRef = inject(MatDialogRef<DespesaFormDialog>);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<DespesaFormDialogData>(MAT_DIALOG_DATA);

  protected readonly despesa = this.data.despesa;
  protected readonly fases: FaseImovel[] = ['LOTE', 'CONSTRUCAO', 'CASA'];
  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly tipoContratoLabel = TIPO_CONTRATO_LABEL;
  protected readonly tipoAnexoLabel = TIPO_ANEXO_DESPESA_LABEL;
  protected readonly tiposAnexo: TipoAnexoDespesa[] = ['COMPROVANTE', 'NOTA_FISCAL', 'RECIBO', 'CONTRATO', 'OUTRO'];

  protected readonly imoveis = signal<ImovelResponseDTO[]>([]);
  protected readonly categorias = signal<CategoriaDespesaResponseDTO[]>([]);
  protected readonly pessoas = signal<PessoaResponseDTO[]>([]);
  protected readonly contratos = signal<ContratoFinanceiroResponseDTO[]>([]);
  protected readonly salvando = signal(false);

  protected readonly anexos = signal<DespesaAnexoResponseDTO[]>([]);
  protected readonly tipoAnexoSelecionado = signal<TipoAnexoDespesa>('COMPROVANTE');
  protected readonly enviandoAnexo = signal(false);

  protected readonly form = this.fb.group({
    imovelId: [this.despesa?.imovelId ?? (null as number | null)],
    categoriaDespesaId: [this.despesa?.categoriaDespesaId ?? (null as number | null), Validators.required],
    pagadorId: [this.despesa?.pagadorId ?? (null as number | null), Validators.required],
    beneficiarioId: [this.despesa?.beneficiarioId ?? (null as number | null)],
    contratoFinanceiroId: [this.despesa?.contratoFinanceiroId ?? (null as number | null)],
    faseImovel: [this.despesa?.faseImovel ?? (null as FaseImovel | null)],
    valor: [this.formatarMoeda(this.despesa?.valor ?? null), Validators.required],
    dataPagamento: [this.despesa?.dataPagamento ?? new Date().toISOString().slice(0, 10), Validators.required],
    descricao: [this.despesa?.descricao ?? ''],
  });

  ngOnInit(): void {
    this.imoveisService.listar().subscribe((imoveis) => this.imoveis.set(imoveis.filter((i) => i.ativo)));
    this.categoriasService.listar().subscribe((categorias) => this.categorias.set(categorias));
    this.pessoasService.listar().subscribe((pessoas) => this.pessoas.set(pessoas.filter((p) => p.ativo)));

    this.carregarContratos(this.form.controls.imovelId.value);
    this.form.controls.imovelId.valueChanges.subscribe((imovelId) => {
      this.form.controls.contratoFinanceiroId.setValue(null);
      this.carregarContratos(imovelId);
    });

    if (this.despesa) {
      this.service.listarAnexos(this.despesa.id).subscribe((anexos) => this.anexos.set(anexos));
    }
  }

  protected salvar(): void {
    if (this.form.invalid) {
      return;
    }

    this.salvando.set(true);

    const bruto = this.form.getRawValue();
    const dto = {
      ...bruto,
      valor: this.parseMoeda(bruto.valor)!,
      descricao: bruto.descricao || null,
    } as DespesaRequestDTO;
    const requisicao = this.despesa ? this.service.atualizar(this.despesa.id, dto) : this.service.criar(dto);

    requisicao.subscribe({
      next: () => {
        this.snackBar.open('Despesa salva com sucesso.', 'Fechar', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        const mensagem = erro.error?.mensagem ?? 'Não foi possível salvar a despesa.';
        this.snackBar.open(mensagem, 'Fechar', { duration: 6000 });
      },
    });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }

  protected formatarCampoValor(): void {
    const controle = this.form.controls.valor;
    controle.setValue(this.formatarMoeda(this.parseMoeda(controle.value)), { emitEvent: false });
  }

  protected enviarAnexo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo || !this.despesa) {
      return;
    }

    this.enviandoAnexo.set(true);
    this.service.adicionarAnexo(this.despesa.id, arquivo, this.tipoAnexoSelecionado()).subscribe({
      next: (anexo) => {
        this.anexos.update((atuais) => [...atuais, anexo]);
        this.enviandoAnexo.set(false);
        input.value = '';
      },
      error: (erro: HttpErrorResponse) => {
        this.enviandoAnexo.set(false);
        input.value = '';
        this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível enviar o anexo.', 'Fechar', { duration: 6000 });
      },
    });
  }

  protected abrirAnexo(anexo: DespesaAnexoResponseDTO): void {
    this.service.baixarAnexo(anexo.url).subscribe((blob) => {
      window.open(URL.createObjectURL(blob), '_blank');
    });
  }

  protected removerAnexo(anexo: DespesaAnexoResponseDTO): void {
    if (!this.despesa || !confirm(`Remover este anexo (${this.tipoAnexoLabel[anexo.tipoAnexo]})?`)) {
      return;
    }
    this.service.deletarAnexo(this.despesa.id, anexo.id).subscribe(() => {
      this.anexos.update((atuais) => atuais.filter((a) => a.id !== anexo.id));
    });
  }

  private carregarContratos(imovelId: number | null): void {
    if (imovelId == null) {
      this.contratos.set([]);
      return;
    }
    this.contratosService.listar(imovelId).subscribe((contratos) => this.contratos.set(contratos));
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
