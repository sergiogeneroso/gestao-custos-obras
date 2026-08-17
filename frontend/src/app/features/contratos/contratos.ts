import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { ContratoDetalheDialog } from './contrato-detalhe-dialog/contrato-detalhe-dialog';
import { ContratoFormDialog } from './contrato-form-dialog/contrato-form-dialog';
import { ContratoFinanceiroResponseDTO, SITUACAO_CONTRATO_LABEL, TIPO_CONTRATO_LABEL } from './contrato.model';
import { ContratosService } from './contratos.service';

@Component({
  selector: 'app-contratos',
  imports: [CurrencyPipe, MatButtonModule, BuscaToolbar],
  templateUrl: './contratos.html',
  styleUrl: './contratos.scss',
})
export class Contratos implements OnInit {
  private readonly service = inject(ContratosService);
  private readonly dialog = inject(MatDialog);

  protected readonly contratos = signal<ContratoFinanceiroResponseDTO[]>([]);
  protected readonly carregando = signal(true);
  protected readonly tipoLabel = TIPO_CONTRATO_LABEL;
  protected readonly situacaoLabel = SITUACAO_CONTRATO_LABEL;

  protected readonly busca = signal('');

  protected readonly contratosFiltrados = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    if (!termo) {
      return this.contratos();
    }
    return this.contratos().filter((contrato) =>
      [contrato.imovelIdentificador, contrato.contraparteNome].some((valor) => valor.toLowerCase().includes(termo)),
    );
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected novo(): void {
    this.dialog
      .open(ContratoFormDialog, { autoFocus: false, width: '680px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.carregar());
  }

  protected verDetalhe(contrato: ContratoFinanceiroResponseDTO): void {
    this.dialog
      .open(ContratoDetalheDialog, { data: { contrato }, autoFocus: false, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.carregar());
  }

  private carregar(): void {
    this.carregando.set(true);
    this.service.listar().subscribe((contratos) => {
      this.contratos.set(contratos);
      this.carregando.set(false);
    });
  }
}
