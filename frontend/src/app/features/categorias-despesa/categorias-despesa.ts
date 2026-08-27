import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { BuscaToolbar } from '../../shared/busca-toolbar/busca-toolbar';
import { ListagemPaginada } from '../../shared/pagina/listagem-paginada';
import { CategoriaDespesaFormDialog } from './categoria-despesa-form-dialog/categoria-despesa-form-dialog';
import { CategoriaDespesaResponseDTO } from './categoria-despesa.model';
import { CategoriasDespesaService } from './categorias-despesa.service';

@Component({
  selector: 'app-categorias-despesa',
  imports: [MatButtonModule, MatPaginatorModule, BuscaToolbar],
  templateUrl: './categorias-despesa.html',
  styleUrl: './categorias-despesa.scss',
})
export class CategoriasDespesa {
  private readonly service = inject(CategoriasDespesaService);
  private readonly dialog = inject(MatDialog);

  protected readonly busca = signal('');

  // A busca é resolvida no backend junto com a paginação: filtrar só a página carregada
  // esconderia registros que casam com o termo mas estão em outra página.
  protected readonly lista = new ListagemPaginada<CategoriaDespesaResponseDTO>(
    inject(DestroyRef),
    (pagina, tamanho) => this.service.listarPagina(this.busca().trim(), pagina, tamanho),
  );

  constructor() {
    effect(() => {
      this.busca();
      this.lista.reiniciar();
    });
  }

  protected mudarPagina(evento: PageEvent): void {
    this.lista.mudarPagina(evento);
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
    this.service.deletar(categoria.id).subscribe(() => this.lista.carregar());
  }

  private abrirFormulario(categoria: CategoriaDespesaResponseDTO | null): void {
    this.dialog
      .open(CategoriaDespesaFormDialog, { data: { categoria }, autoFocus: false, width: '480px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe(() => this.lista.carregar());
  }
}
