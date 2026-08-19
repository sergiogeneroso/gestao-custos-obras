import { CurrencyPipe, DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SITUACAO_CONTRATO_LABEL, TIPO_CONTRATO_LABEL } from '../contratos/contrato.model';
import { ETAPA_CONSTRUCAO_LABEL, ETAPAS_CONSTRUCAO } from '../despesas/despesa.model';
import {
  FASE_IMOVEL_LABEL,
  FaseImovel,
  ImovelResponseDTO,
  SITUACAO_IMOVEL_LABEL,
} from '../imoveis/imovel.model';
import { ImoveisService } from '../imoveis/imoveis.service';
import { PosicaoContratoDTO, ResultadoImovelDTO } from './relatorio.model';
import { RelatoriosService } from './relatorios.service';

@Component({
  selector: 'app-relatorios',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    PercentPipe,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  templateUrl: './relatorios.html',
  styleUrl: './relatorios.scss',
})
export class Relatorios implements OnInit {
  private readonly service = inject(RelatoriosService);
  private readonly imoveisService = inject(ImoveisService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly faseLabel = FASE_IMOVEL_LABEL;
  protected readonly situacaoLabel = SITUACAO_IMOVEL_LABEL;
  protected readonly tipoContratoLabel = TIPO_CONTRATO_LABEL;
  protected readonly situacaoContratoLabel = SITUACAO_CONTRATO_LABEL;
  protected readonly fases: FaseImovel[] = ['LOTE', 'CONSTRUCAO', 'CASA'];
  protected readonly etapas = ETAPAS_CONSTRUCAO;
  protected readonly etapaLabel = ETAPA_CONSTRUCAO_LABEL;

  protected readonly imoveis = signal<ImovelResponseDTO[]>([]);
  protected readonly imovelSelecionadoId = signal<number | null>(null);
  protected readonly resultado = signal<ResultadoImovelDTO | null>(null);
  protected readonly carregando = signal(true);

  protected readonly hoje = new Date();

  // Só há bloco de obra quando existe estimativa ou despesa de construção lançada.
  protected readonly temBlocoObra = computed(() => {
    const r = this.resultado();
    return !!r && (r.custoEstimadoObra !== null || r.custoRealObra > 0);
  });

  ngOnInit(): void {
    this.imoveisService.listar().subscribe((imoveis) => {
      this.imoveis.set(imoveis);
      const primeiro = imoveis[0];
      if (primeiro) {
        this.selecionar(primeiro.id);
      } else {
        this.carregando.set(false);
      }
    });
  }

  protected selecionar(imovelId: number): void {
    this.imovelSelecionadoId.set(imovelId);
    this.carregando.set(true);
    this.service.resultado(imovelId).subscribe({
      next: (resultado) => {
        this.resultado.set(resultado);
        this.carregando.set(false);
      },
      error: (erro: HttpErrorResponse) => {
        this.carregando.set(false);
        this.snackBar.open(erro.error?.mensagem ?? 'Não foi possível carregar o resultado.', 'Fechar', {
          duration: 6000,
        });
      },
    });
  }

  protected exportarCsv(): void {
    const resultado = this.resultado();
    if (!resultado) {
      return;
    }

    this.service.exportarCsv(resultado.imovelId).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `resultado-${resultado.identificador}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    });
  }

  // O navegador oferece "Salvar como PDF" no próprio diálogo; o layout de papel
  // está no @media print de relatorios.scss.
  protected salvarPdf(): void {
    window.print();
  }

  protected rotuloSaldo(contrato: PosicaoContratoDTO): string {
    return contrato.tipo === 'PARCELAMENTO_VENDA' ? 'a receber' : 'saldo devedor';
  }
}

