import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { DespesaFormDialog } from '../despesa-form-dialog/despesa-form-dialog';
import { DespesaPainelDetalhe } from '../despesa-painel-detalhe/despesa-painel-detalhe';
import { DespesaResponseDTO } from '../despesa.model';
import { HistoricoDialog } from '../../../shared/auditoria/historico-dialog/historico-dialog';

export interface DespesaDetalheDialogData {
  /** A página carregada da tabela, para o diálogo percorrê-la sem voltar ao servidor. */
  despesas: DespesaResponseDTO[];
  indice: number;
}

// Abrir uma despesa é consulta, não edição: a lista abre este diálogo e a edição fica atrás de um
// botão explícito, para não expor os campos a alteração acidental num clique de linha. O
// formulário abre por cima em vez de fechar o diálogo, para não perder o lugar na lista de quem
// está percorrendo os lançamentos com as setas.
@Component({
  selector: 'app-despesa-detalhe-dialog',
  imports: [MatButtonModule, MatDialogModule, DespesaPainelDetalhe],
  templateUrl: './despesa-detalhe-dialog.html',
  styleUrl: './despesa-detalhe-dialog.scss',
})
export class DespesaDetalheDialog {
  private readonly dialog = inject(MatDialog);
  private readonly dialogRef = inject(MatDialogRef<DespesaDetalheDialog>);
  private readonly data = inject<DespesaDetalheDialogData>(MAT_DIALOG_DATA);

  protected readonly despesas = signal<DespesaResponseDTO[]>(this.data.despesas);
  protected readonly indice = signal(this.data.indice);
  protected readonly despesa = computed(() => this.despesas()[this.indice()]);

  protected anterior(): void {
    this.indice.update((atual) => Math.max(0, atual - 1));
  }

  protected proxima(): void {
    this.indice.update((atual) => Math.min(this.despesas().length - 1, atual + 1));
  }

  protected editar(): void {
    this.dialog
      .open(DespesaFormDialog, {
        data: { despesa: this.despesa() },
        autoFocus: false,
        width: '640px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((salva?: DespesaResponseDTO) => {
        if (!salva) {
          return;
        }
        // Troca só a despesa editada na lista local: o painel reage à nova referência e reexibe os
        // campos, sem perder a posição de quem estava percorrendo a página.
        this.despesas.update((atuais) => atuais.map((d) => (d.id === salva.id ? salva : d)));
      });
  }

  protected verHistorico(): void {
    this.dialog.open(HistoricoDialog, {
      data: { entidade: 'Despesa', entidadeId: this.despesa().id, titulo: this.despesa().descricao || `#${this.despesa().id}` },
      autoFocus: false,
      width: '640px',
      maxWidth: '95vw',
    });
  }

  protected fechar(): void {
    this.dialogRef.close();
  }
}
