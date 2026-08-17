import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { DespesaFormDialog } from './despesa-form-dialog/despesa-form-dialog';
import { DespesaResponseDTO } from './despesa.model';
import { DespesasService } from './despesas.service';

@Component({
  selector: 'app-despesas',
  imports: [CurrencyPipe, DatePipe, MatButtonModule, MatButtonToggleModule, BuscaToolbar],
  templateUrl: './despesas.html',
  styleUrl: './despesas.scss',
})
export class Despesas implements OnInit {
  private readonly service = inject(DespesasService);
  private readonly dialog = inject(MatDialog);

  protected readonly despesas = signal<DespesaResponseDTO[]>([]);
  protected readonly carregando = signal(true);

  protected readonly busca = signal('');
  protected readonly filtro = signal<'todas' | 'imovel' | 'geral'>('todas');

  protected readonly despesasFiltradas = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    const filtro = this.filtro();

    return this.despesas().filter((despesa) => {
      if (filtro === 'imovel' && despesa.imovelId === null) return false;
      if (filtro === 'geral' && despesa.imovelId !== null) return false;
      if (!termo) return true;
      return [despesa.categoriaDespesaNome, despesa.pagadorNome, despesa.beneficiarioNome, despesa.imovelIdentificador, despesa.descricao]
        .filter((valor): valor is string => !!valor)
        .some((valor) => valor.toLowerCase().includes(termo));
    });
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected novo(): void {
    this.abrirFormulario(null);
  }

  protected editar(despesa: DespesaResponseDTO): void {
    this.abrirFormulario(despesa);
  }

  protected inativar(despesa: DespesaResponseDTO): void {
    if (!confirm(`Inativar esta despesa de ${despesa.categoriaDespesaNome}?`)) {
      return;
    }
    this.service.inativar(despesa.id).subscribe(() => this.carregar());
  }

  private abrirFormulario(despesa: DespesaResponseDTO | null): void {
    this.dialog
      .open(DespesaFormDialog, { data: { despesa }, autoFocus: false, width: '640px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.carregar());
  }

  private carregar(): void {
    this.carregando.set(true);
    this.service.listar().subscribe((despesas) => {
      this.despesas.set(despesas);
      this.carregando.set(false);
    });
  }
}
