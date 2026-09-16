import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  ElementRef,
  OnDestroy,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ConfirmDialog } from '../../../shared/confirm-dialog/confirm-dialog';
import { mensagemErro } from '../../../shared/erro/erro.util';
import { FASE_IMOVEL_LABEL } from '../../imoveis/imovel.model';
import {
  DespesaAnexoResponseDTO,
  DespesaResponseDTO,
  ETAPA_CONSTRUCAO_LABEL,
  TIPO_ANEXO_DESPESA_LABEL,
  TipoAnexoDespesa,
  tipoArquivoAnexo,
} from '../despesa.model';
import { DespesasService } from '../despesas.service';

/** Quantos anexos a despesa tem, emitido ao carregar e a cada anexo enviado ou removido. O id vem
 * junto porque a despesa exibida pode ter mudado enquanto a requisição estava em voo, e quem
 * recebe precisa atualizar a linha certa. */
export interface ContagemAnexos {
  despesaId: number;
  quantidade: number;
}

// Exibe uma despesa e cuida dos anexos dela — nada mais. Não conhece a coleção em que a despesa
// está: navegar entre despesas e oferecer o botão de editar é decisão de quem hospeda o painel,
// porque a aba do imóvel percorre uma lista agrupada por fase e a tela de despesas percorre uma
// página vinda do servidor.
@Component({
  selector: 'app-despesa-painel-detalhe',
  imports: [CurrencyPipe, DatePipe, MatButtonModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './despesa-painel-detalhe.html',
  styleUrl: './despesa-painel-detalhe.scss',
})
export class DespesaPainelDetalhe implements OnDestroy {
  private readonly service = inject(DespesasService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly despesa = input.required<DespesaResponseDTO>();
  /** A aba do imóvel já está escopada a um imóvel, e repetir o identificador ali seria ruído; a
   * tela de despesas mistura imóveis e gastos gerais, então lá o campo é indispensável. */
  readonly mostrarImovel = input(false);
  readonly contagemAnexos = output<ContagemAnexos>();

  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly etapaLabel = ETAPA_CONSTRUCAO_LABEL;
  protected readonly tipoAnexoLabel = TIPO_ANEXO_DESPESA_LABEL;
  protected readonly tiposAnexo: TipoAnexoDespesa[] = [
    'COMPROVANTE',
    'NOTA_FISCAL',
    'RECIBO',
    'CONTRATO',
    'OUTRO',
  ];
  protected readonly formato = tipoArquivoAnexo;

  protected readonly anexos = signal<DespesaAnexoResponseDTO[]>([]);
  protected readonly previewImagens = signal<Record<number, string>>({});
  protected readonly previewPdfs = signal<Record<number, SafeResourceUrl>>({});
  protected readonly tipoAnexoSelecionado = signal<TipoAnexoDespesa>('COMPROVANTE');
  protected readonly enviando = signal(false);
  protected readonly ampliada = signal<string | null>(null);

  private readonly lupa = viewChild.required<ElementRef<HTMLDialogElement>>('lupa');

  private objectUrls: string[] = [];

  constructor() {
    effect(() => this.carregarAnexos(this.despesa().id));
  }

  ngOnDestroy(): void {
    this.objectUrls.forEach((url) => URL.revokeObjectURL(url));
  }

  protected enviarAnexo(event: Event): void {
    const elementoInput = event.target as HTMLInputElement;
    const arquivo = elementoInput.files?.[0];
    if (!arquivo) {
      return;
    }
    const despesaId = this.despesa().id;
    const quantidadeAntes = this.anexos().length;

    this.enviando.set(true);
    this.service.adicionarAnexo(despesaId, arquivo, this.tipoAnexoSelecionado()).subscribe({
      next: (anexo) => {
        // A despesa exibida pode ter mudado enquanto o upload estava em voo: o anexo já foi
        // gravado na despesa certa no backend, mas só mexe na lista local se ainda for a exibida.
        if (despesaId === this.despesa().id) {
          this.anexos.update((atuais) => [...atuais, anexo]);
          this.carregarPreview(anexo);
        }
        this.contagemAnexos.emit({ despesaId, quantidade: quantidadeAntes + 1 });
        this.enviando.set(false);
        elementoInput.value = '';
      },
      error: (erro: HttpErrorResponse) => {
        this.enviando.set(false);
        elementoInput.value = '';
        this.snackBar.open(mensagemErro(erro, 'Não foi possível enviar o anexo.'), 'Fechar', {
          duration: 6000,
        });
      },
    });
  }

  protected removerAnexo(anexo: DespesaAnexoResponseDTO): void {
    this.dialog
      .open(ConfirmDialog, {
        data: { titulo: `Remover este anexo (${this.tipoAnexoLabel[anexo.tipoAnexo]})?` },
        autoFocus: false,
        width: '420px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((confirmado?: boolean) => {
        if (!confirmado) {
          return;
        }
        this.confirmarRemocaoAnexo(anexo);
      });
  }

  private confirmarRemocaoAnexo(anexo: DespesaAnexoResponseDTO): void {
    const despesaId = this.despesa().id;
    const quantidadeAntes = this.anexos().length;

    this.service.deletarAnexo(despesaId, anexo.id).subscribe(() => {
      if (despesaId === this.despesa().id) {
        this.anexos.update((atuais) => atuais.filter((a) => a.id !== anexo.id));
      }
      this.contagemAnexos.emit({ despesaId, quantidade: Math.max(0, quantidadeAntes - 1) });
    });
  }

  // A miniatura na lista existe para reconhecer o comprovante; ler o valor de uma nota fotografada
  // exige a imagem inteira, e é isso que o clique abre.
  protected ampliar(url: string): void {
    this.ampliada.set(url);
    this.lupa().nativeElement.showModal();
  }

  // O overlay do Material cancela a ação padrão do Esc, então o <dialog> nativo não fecha sozinho
  // enquanto está dentro dele. Fecha na mão e segura o evento, para o diálogo que hospeda o painel
  // não fechar junto no mesmo Esc.
  protected fecharAmpliada(evento: Event): void {
    evento.stopPropagation();
    this.lupa().nativeElement.close();
  }

  protected baixarAnexo(anexo: DespesaAnexoResponseDTO): void {
    this.service.baixarAnexo(anexo.url).subscribe((blob) => {
      window.open(URL.createObjectURL(blob), '_blank');
    });
  }

  private carregarAnexos(despesaId: number): void {
    this.service.listarAnexos(despesaId).subscribe((anexos) => {
      // Trocar de despesa rápido deixa mais de uma listagem em voo; a resposta atrasada de uma
      // despesa que não está mais na tela substituiria os anexos da despesa exibida.
      if (despesaId !== this.despesa().id) {
        return;
      }
      this.anexos.set(anexos);
      this.contagemAnexos.emit({ despesaId, quantidade: anexos.length });
      anexos
        .filter(
          (a) =>
            this.formato(a.url) !== 'outro' &&
            !(a.id in this.previewImagens()) &&
            !(a.id in this.previewPdfs()),
        )
        .forEach((a) => this.carregarPreview(a));
    });
  }

  private carregarPreview(anexo: DespesaAnexoResponseDTO): void {
    const formato = this.formato(anexo.url);
    if (formato === 'outro') {
      return;
    }
    this.service.baixarAnexo(anexo.url).subscribe((blob) => {
      const objectUrl = URL.createObjectURL(blob);
      this.objectUrls.push(objectUrl);
      if (formato === 'imagem') {
        this.previewImagens.update((atuais) => ({ ...atuais, [anexo.id]: objectUrl }));
      } else {
        this.previewPdfs.update((atuais) => ({
          ...atuais,
          [anexo.id]: this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl),
        }));
      }
    });
  }
}
