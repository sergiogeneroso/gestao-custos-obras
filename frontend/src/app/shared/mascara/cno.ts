import { Mascara } from './mascara.directive';

/**
 * CNO (Cadastro Nacional de Obras): 12 dígitos, `00.000.00000/00`, gravado sem
 * pontuação. Só máscara — o backend não confere o dígito verificador.
 */

export function formatarCno(cno: string): string {
  const d = cno.replace(/\D/g, '').slice(0, 12);
  if (d.length <= 2) return d;
  if (d.length <= 5) return `${d.slice(0, 2)}.${d.slice(2)}`;
  if (d.length <= 10) return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5)}`;
  return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 10)}/${d.slice(10)}`;
}

/**
 * Para exibição. O CNO era texto livre antes da máscara: o que não tem 12 dígitos
 * aparece como foi gravado, em vez de ser recortado num formato que não é o dele.
 */
export function exibirCno(cno: string | null | undefined): string {
  if (!cno) return '';
  return cno.replace(/\D/g, '').length === 12 ? formatarCno(cno) : cno;
}

export const MASCARA_CNO: Mascara = {
  limpar: (bruto) => bruto.replace(/\D/g, ''),
  formatar: formatarCno,
  maxLimpo: 12,
};
