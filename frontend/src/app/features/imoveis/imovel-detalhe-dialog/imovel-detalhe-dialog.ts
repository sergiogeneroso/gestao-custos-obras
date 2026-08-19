import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { SITUACAO_CONTRATO_LABEL, TIPO_CONTRATO_LABEL } from '../../contratos/contrato.model';
import { DespesaResponseDTO } from '../../despesas/despesa.model';
import { DespesaFormDialog } from '../../despesas/despesa-form-dialog/despesa-form-dialog';
import { DespesasService } from '../../despesas/despesas.service';
import { PosicaoContratoDTO, ResultadoImovelDTO } from '../../relatorios/relatorio.model';
import { RelatoriosService } from '../../relatorios/relatorios.service';
import {
  EnvioDocumento,
  FASE_IMOVEL_LABEL,
  FaseImovel,
  ImovelDocumentoResponseDTO,
  ImovelFotoResponseDTO,
  ImovelResponseDTO,
  PROXIMA_FASE,
  SITUACAO_IMOVEL_LABEL,
  TIPO_DOCUMENTO_IMOVEL_LABEL,
  TipoDocumentoImovel,
} from '../imovel.model';
import { ImoveisService } from '../imoveis.service';
import { ImovelAVendaDialog } from '../imovel-a-venda-dialog/imovel-a-venda-dialog';
import { ImovelFaseDialog } from '../imovel-fase-dialog/imovel-fase-dialog';
import { ImovelVendaDialog } from '../imovel-venda-dialog/imovel-venda-dialog';

export interface ImovelDetalheDialogData {
  imovel: ImovelResponseDTO;
}

@Component({
  selector: 'app-imovel-detalhe-dialog',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, MatButtonModule, MatDialogModule, MatTabsModule],
  templateUrl: './imovel-detalhe-dialog.html',
  styleUrl: './imovel-detalhe-dialog.scss',
})
export class ImovelDetalheDialog implements OnInit, OnDestroy {
  private readonly service = inject(ImoveisService);
  private readonly despesasService = inject(DespesasService);
  private readonly relatoriosService = inject(RelatoriosService);
  private readonly dialog = inject(MatDialog);
  protected readonly data = inject<ImovelDetalheDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = signal(this.data.imovel);
  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly situacaoLabel = SITUACAO_IMOVEL_LABEL;
  protected readonly tipoContratoLabel = TIPO_CONTRATO_LABEL;
  protected readonly situacaoContratoLabel = SITUACAO_CONTRATO_LABEL;
  protected readonly fases: FaseImovel[] = ['LOTE', 'CONSTRUCAO', 'CASA'];
  protected readonly proximaFase = computed(() => PROXIMA_FASE[this.imovel().fase]);

  protected readonly fotos = signal<ImovelFotoResponseDTO[]>([]);
  protected readonly urlsFotos = signal<Record<number, string>>({});
  protected readonly indiceAtual = signal(0);

  protected readonly resultado = signal<ResultadoImovelDTO | null>(null);
  protected readonly despesas = signal<DespesaResponseDTO[]>([]);

  protected readonly documentos = signal<ImovelDocumentoResponseDTO[]>([]);
  protected readonly tipoDocumentoLabel = TIPO_DOCUMENTO_IMOVEL_LABEL;
  protected readonly tiposDocumento = Object.keys(TIPO_DOCUMENTO_IMOVEL_LABEL) as TipoDocumentoImovel[];
  protected readonly filtroFaseDocumento = signal<FaseImovel | ''>('');
  protected readonly enviandoDocumento = signal(false);
  protected readonly novoDocumento = signal<EnvioDocumento>(this.envioVazio());

  protected readonly documentosFiltrados = computed(() => {
    const fase = this.filtroFaseDocumento();
    const lista = fase ? this.documentos().filter((d) => d.faseImovel === fase) : this.documentos();
    return [...lista].sort((a, b) => b.dataUpload.localeCompare(a.dataUpload));
  });

  protected readonly fotoAtual = computed(() => this.fotos()[this.indiceAtual()] ?? null);

  // As mais recentes primeiro; a lista completa continua na tela de despesas.
  protected readonly despesasRecentes = computed(() =>
    [...this.despesas()].sort((a, b) => b.dataPagamento.localeCompare(a.dataPagamento)).slice(0, 5),
  );

  ngOnInit(): void {
    this.service.listarFotos(this.imovel().id).subscribe((fotos) => {
      this.fotos.set(fotos);
      fotos.forEach((foto) => this.carregarUrlFoto(foto));
    });

    this.carregarFinanceiro();
    this.carregarDocumentos();
  }

  ngOnDestroy(): void {
    Object.values(this.urlsFotos()).forEach((url) => URL.revokeObjectURL(url));
  }

  protected anterior(): void {
    const total = this.fotos().length;
    this.indiceAtual.update((i) => (i - 1 + total) % total);
  }

  protected proxima(): void {
    const total = this.fotos().length;
    this.indiceAtual.update((i) => (i + 1) % total);
  }

  protected avancarFase(): void {
    this.dialog
      .open(ImovelFaseDialog, { data: { imovel: this.imovel() }, autoFocus: false, width: '420px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((atualizado: ImovelResponseDTO | undefined) => {
        if (atualizado) {
          this.imovel.set(atualizado);
        }
      });
  }

  protected colocarAVenda(): void {
    this.dialog
      .open(ImovelAVendaDialog, { data: { imovel: this.imovel() }, autoFocus: false, width: '420px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((atualizado: ImovelResponseDTO | undefined) => {
        if (atualizado) {
          this.imovel.set(atualizado);
        }
      });
  }

  protected marcarComoAdquirido(): void {
    this.service
      .alterarSituacao(this.imovel().id, { novaSituacao: 'ADQUIRIDO', valorVenda: null, dataVenda: null, compradorId: null })
      .subscribe((atualizado) => this.imovel.set(atualizado));
  }

  protected registrarVenda(): void {
    this.dialog
      .open(ImovelVendaDialog, { data: { imovel: this.imovel() }, autoFocus: false, width: '420px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((atualizado: ImovelResponseDTO | undefined) => {
        if (atualizado) {
          this.imovel.set(atualizado);
        }
      });
  }

  // Custo e contratos vêm prontos de resultado-imovel — a mesma fonte da tela de resultado, para
  // não existir uma segunda conta de custo no frontend.
  private carregarFinanceiro(): void {
    this.relatoriosService.resultado(this.imovel().id).subscribe((resultado) => this.resultado.set(resultado));
    this.despesasService.listar(this.imovel().id).subscribe((despesas) => this.despesas.set(despesas));
  }

  protected lancarDespesa(): void {
    this.dialog
      .open(DespesaFormDialog, {
        data: { despesa: null, imovelId: this.imovel().id },
        autoFocus: false,
        width: '680px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe(() => this.carregarFinanceiro());
  }

  protected rotuloSaldo(contrato: PosicaoContratoDTO): string {
    return contrato.tipo === 'PARCELAMENTO_VENDA' ? 'a receber' : 'saldo devedor';
  }

  private carregarDocumentos(): void {
    this.service.listarDocumentos(this.imovel().id).subscribe((documentos) => this.documentos.set(documentos));
  }

  private envioVazio(): EnvioDocumento {
    // A fase atual é só sugestão: documento antigo pode ser anexado depois, já em outra fase.
    return {
      tipoDocumento: 'MATRICULA',
      faseImovel: this.data.imovel.fase,
      descricao: null,
      dataEmissao: null,
      dataValidade: null,
    };
  }

  protected atualizarEnvio(campo: keyof EnvioDocumento, valor: string): void {
    this.novoDocumento.update((atual) => ({ ...atual, [campo]: valor || null }));
  }

  protected enviarDocumento(event: Event): void {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo) {
      return;
    }

    this.enviandoDocumento.set(true);
    this.service.adicionarDocumento(this.imovel().id, arquivo, this.novoDocumento()).subscribe({
      next: (documento) => {
        this.documentos.update((atuais) => [...atuais, documento]);
        this.novoDocumento.set(this.envioVazio());
        this.enviandoDocumento.set(false);
        input.value = '';
      },
      error: () => {
        this.enviandoDocumento.set(false);
        input.value = '';
      },
    });
  }

  protected abrirDocumento(documento: ImovelDocumentoResponseDTO): void {
    this.service.baixarDocumento(documento.url).subscribe((blob) => {
      window.open(URL.createObjectURL(blob), '_blank');
    });
  }

  protected removerDocumento(documento: ImovelDocumentoResponseDTO): void {
    if (!confirm(`Remover o documento "${documento.nomeArquivo ?? documento.id}"?`)) {
      return;
    }
    this.service.deletarDocumento(this.imovel().id, documento.id).subscribe(() => {
      this.documentos.update((atuais) => atuais.filter((d) => d.id !== documento.id));
    });
  }

  protected vencido(documento: ImovelDocumentoResponseDTO): boolean {
    return !!documento.dataValidade && documento.dataValidade < new Date().toISOString().slice(0, 10);
  }

  private carregarUrlFoto(foto: ImovelFotoResponseDTO): void {
    this.service.baixarFoto(foto.url).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      this.urlsFotos.update((atuais) => ({ ...atuais, [foto.id]: url }));
    });
  }
}
