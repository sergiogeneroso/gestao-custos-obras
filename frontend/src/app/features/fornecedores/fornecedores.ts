import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { FornecedorFormDialog } from './fornecedor-form-dialog/fornecedor-form-dialog';
import { FornecedorResponseDTO } from './fornecedor.model';
import { FornecedoresService } from './fornecedores.service';

@Component({
  selector: 'app-fornecedores',
  imports: [MatButtonModule, BuscaToolbar],
  templateUrl: './fornecedores.html',
  styleUrl: './fornecedores.scss',
})
export class Fornecedores implements OnInit {
  private readonly service = inject(FornecedoresService);
  private readonly dialog = inject(MatDialog);

  protected readonly fornecedores = signal<FornecedorResponseDTO[]>([]);
  protected readonly carregando = signal(true);

  protected readonly busca = signal('');

  protected readonly fornecedoresFiltrados = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    if (!termo) {
      return this.fornecedores();
    }
    return this.fornecedores().filter((fornecedor) =>
      [fornecedor.pessoaNome, fornecedor.areaAtuacao]
        .filter((valor): valor is string => !!valor)
        .some((valor) => valor.toLowerCase().includes(termo)),
    );
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected novo(): void {
    this.abrirFormulario(null);
  }

  protected editar(fornecedor: FornecedorResponseDTO): void {
    this.abrirFormulario(fornecedor);
  }

  protected inativar(fornecedor: FornecedorResponseDTO): void {
    if (!confirm(`Inativar o fornecedor "${fornecedor.pessoaNome}"?`)) {
      return;
    }
    this.service.inativar(fornecedor.id).subscribe(() => this.carregar());
  }

  private abrirFormulario(fornecedor: FornecedorResponseDTO | null): void {
    this.dialog
      .open(FornecedorFormDialog, { data: { fornecedor }, autoFocus: false, width: '520px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.carregar());
  }

  private carregar(): void {
    this.carregando.set(true);
    this.service.listar().subscribe((fornecedores) => {
      this.fornecedores.set(fornecedores);
      this.carregando.set(false);
    });
  }
}
