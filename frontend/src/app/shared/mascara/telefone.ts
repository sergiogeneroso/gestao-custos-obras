import { Mascara } from './mascara.directive';

/** Telefone brasileiro: 10 dígitos (fixo) ou 11 (celular). Gravado só com dígitos. */

export function limparTelefone(bruto: string): string {
  return bruto.replace(/[^0-9]/g, '');
}

export function formatarTelefone(telefone: string | null | undefined): string {
  const d = limparTelefone(telefone ?? '').slice(0, 11);
  if (d.length <= 2) return d;
  if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  // Fixo quebra em 4+4, celular em 5+4 — a divisa muda conforme o total.
  const corte = d.length <= 10 ? 6 : 7;
  return `(${d.slice(0, 2)}) ${d.slice(2, corte)}-${d.slice(corte)}`;
}

export const MASCARA_TELEFONE: Mascara = {
  limpar: limparTelefone,
  formatar: formatarTelefone,
  maxLimpo: 11,
};
