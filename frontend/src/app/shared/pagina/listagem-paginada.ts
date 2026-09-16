import { DestroyRef, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, debounceTime, switchMap } from 'rxjs';
import { PaginaDTO, TAMANHOS_PAGINA, TAMANHO_PAGINA_PADRAO } from './pagina.model';

/**
 * Estado de uma listagem paginada no servidor.
 *
 * Existe porque as cinco telas de listagem precisam exatamente do mesmo comportamento: manter
 * página e tamanho, esperar o usuário parar de digitar antes de consultar, descartar a resposta
 * de uma consulta que já foi substituída por outra, e voltar para a primeira página sempre que um
 * filtro muda. Repetir isso cinco vezes é onde o detalhe sutil se perde numa delas.
 *
 * A tela é dona dos seus próprios filtros: eles entram pela closure `buscar`, não por aqui.
 */
export class ListagemPaginada<T> {
  readonly pagina = signal(0);
  readonly tamanho = signal(TAMANHO_PAGINA_PADRAO);
  readonly carregando = signal(true);
  readonly itens = signal<T[]>([]);
  readonly total = signal(0);
  readonly tamanhos = TAMANHOS_PAGINA;

  private readonly pedidos = new Subject<void>();

  constructor(
    destroyRef: DestroyRef,
    private readonly buscar: (pagina: number, tamanho: number) => Observable<PaginaDTO<T>>,
    atrasoMs = 250,
  ) {
    this.pedidos
      .pipe(
        // O atraso serve à digitação na busca; de quebra, clicar rápido em várias páginas dispara
        // uma consulta só. O switchMap garante que uma resposta atrasada nunca sobrescreva a
        // resposta de um pedido mais novo.
        debounceTime(atrasoMs),
        switchMap(() => this.buscar(untracked(this.pagina), untracked(this.tamanho))),
        takeUntilDestroyed(destroyRef),
      )
      .subscribe((resposta) => {
        this.itens.set(resposta.conteudo);
        this.total.set(resposta.totalElementos);
        this.carregando.set(false);
      });
  }

  /** Recarrega a página atual — depois de salvar, editar ou excluir um registro. */
  carregar(): void {
    this.carregando.set(true);
    this.pedidos.next();
  }

  /** Volta para a primeira página e recarrega. Use quando a busca ou um filtro mudar. */
  reiniciar(): void {
    untracked(() => {
      this.pagina.set(0);
      this.carregar();
    });
  }

  /** Handler do `<mat-paginator>`. */
  mudarPagina(evento: { pageIndex: number; pageSize: number }): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
    this.carregar();
  }
}
