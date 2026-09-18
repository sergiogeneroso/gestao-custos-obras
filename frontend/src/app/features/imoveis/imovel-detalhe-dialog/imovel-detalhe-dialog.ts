import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { ConfirmDialog } from '../../../shared/confirm-dialog/confirm-dialog';
import { HistoricoDialog } from '../../../shared/auditoria/historico-dialog/historico-dialog';
import { arquivoDentroDoLimite, MENSAGEM_ARQUIVO_GRANDE } from '../../../shared/arquivo/tamanho-arquivo.util';
import { MascaraDataDirective } from '../../../shared/data/mascara-data.directive';
import { paraData, paraIso } from '../../../shared/data/data.util';
import { exibirCno } from '../../../shared/mascara/cno';
import { formatarCep } from '../../../shared/mascara/cep';
import { SITUACAO_CONTRATO_LABEL, TIPO_CONTRATO_LABEL } from '../../contratos/contrato.model';
import { ImovelDespesasAba } from './imovel-despesas-aba/imovel-despesas-aba';
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
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    ImovelDespesasAba,
    MascaraDataDirective,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatTabsModule,
  ],
  templateUrl: './imovel-detalhe-dialog.html',
  styleUrl: './imovel-detalhe-dialog.scss',
})
export class ImovelDetalheDialog implements OnInit, OnDestroy {
  private readonly service = inject(ImoveisService);
  private readonly relatoriosService = inject(RelatoriosService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly data = inject<ImovelDetalheDialogData>(MAT_DIALOG_DATA);

  protected readonly imovel = signal(this.data.imovel);
  // computed, não paraData() no template: um Date novo a cada ciclo faria o datepicker reescrever o texto no meio da digitação.
  protected readonly dataEmissaoEnvio = computed(() => paraData(this.novoDocumento().dataEmissao));
  protected readonly dataValidadeEnvio = computed(() => paraData(this.novoDocumento().dataValidade));
  protected readonly paraIso = paraIso;
  protected readonly formatarCep = formatarCep;
  protected readonly exibirCno = exibirCno;
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
  protected carregarFinanceiro(): void {
    this.relatoriosService.resultado(this.imovel().id).subscribe((resultado) => this.resultado.set(resultado));
  }

  protected rotuloSaldo(contrato: PosicaoContratoDTO): string {
    return contrato.tipo === 'PARCELAMENTO_VENDA' ? 'a receber' : 'saldo devedor';
  }

  protected verHistorico(): void {
    this.dialog.open(HistoricoDialog, {
      data: { entidade: 'Imovel', entidadeId: this.imovel().id, titulo: this.imovel().identificador },
      autoFocus: false,
      width: '640px',
      maxWidth: '95vw',
    });
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

  protected atualizarEnvio(campo: keyof EnvioDocumento, valor: string | null): void {
    this.novoDocumento.update((atual) => ({ ...atual, [campo]: valor || null }));
  }

  protected enviarDocumento(event: Event): void {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo) {
      return;
    }
    if (!arquivoDentroDoLimite(arquivo)) {
      input.value = '';
      this.snackBar.open(MENSAGEM_ARQUIVO_GRANDE, 'Fechar', { duration: 6000 });
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
    this.dialog
      .open(ConfirmDialog, {
        data: { titulo: `Remover o documento "${documento.nomeArquivo ?? documento.id}"?` },
        autoFocus: false,
        width: '420px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((confirmado?: boolean) => {
        if (!confirmado) {
          return;
        }
        this.service.deletarDocumento(this.imovel().id, documento.id).subscribe(() => {
          this.documentos.update((atuais) => atuais.filter((d) => d.id !== documento.id));
        });
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
