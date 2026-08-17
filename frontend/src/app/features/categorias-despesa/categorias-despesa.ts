import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { CategoriaDespesaFormDialog } from './categoria-despesa-form-dialog/categoria-despesa-form-dialog';
import { CategoriaDespesaResponseDTO } from './categoria-despesa.model';
import { CategoriasDespesaService } from './categorias-despesa.service';

@Component({
  selector: 'app-categorias-despesa',
  imports: [MatButtonModule, BuscaToolbar],
  templateUrl: './categorias-despesa.html',
  styleUrl: './categorias-despesa.scss',
})
export class CategoriasDespesa implements OnInit {
  private readonly service = inject(CategoriasDespesaService);
  private readonly dialog = inject(MatDialog);

  protected readonly categorias = signal<CategoriaDespesaResponseDTO[]>([]);
  protected readonly carregando = signal(true);

  protected readonly busca = signal('');

  protected readonly categoriasFiltradas = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    if (!termo) {
      return this.categorias();
    }
    return this.categorias().filter((categoria) =>
      [categoria.nome, categoria.descricao]
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

  protected editar(categoria: CategoriaDespesaResponseDTO): void {
    this.abrirFormulario(categoria);
  }

  protected excluir(categoria: CategoriaDespesaResponseDTO): void {
    if (!confirm(`Excluir a categoria "${categoria.nome}"?`)) {
      return;
    }
    this.service.deletar(categoria.id).subscribe(() => this.carregar());
  }

  private abrirFormulario(categoria: CategoriaDespesaResponseDTO | null): void {
    this.dialog
      .open(CategoriaDespesaFormDialog, { data: { categoria }, autoFocus: false, width: '480px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.carregar());
  }

  private carregar(): void {
    this.carregando.set(true);
    this.service.listar().subscribe((categorias) => {
      this.categorias.set(categorias);
      this.carregando.set(false);
    });
  }
}
