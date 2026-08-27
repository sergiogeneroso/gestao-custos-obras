/**
 * Página devolvida pelos endpoints `/pagina` (ver PaginaDTO no backend).
 *
 * As telas de listagem usam esses endpoints; os combos dos formulários continuam usando o `listar()`
 * de cada service, que traz a lista inteira — um select paginado esconderia opções válidas.
 */
export interface PaginaDTO<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
}

export const TAMANHO_PAGINA_PADRAO = 20;
export const TAMANHOS_PAGINA = [10, 20, 50, 100];

export function paginaVazia<T>(tamanho = TAMANHO_PAGINA_PADRAO): PaginaDTO<T> {
  return { conteudo: [], pagina: 0, tamanho, totalElementos: 0, totalPaginas: 0 };
}
