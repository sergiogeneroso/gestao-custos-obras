import { Mascara } from './mascara.directive';

/** CEP: 8 dígitos, gravado sem o hífen. */

export function formatarCep(cep: string | null | undefined): string {
  const d = (cep ?? '').replace(/\D/g, '').slice(0, 8);
  return d.length <= 5 ? d : `${d.slice(0, 5)}-${d.slice(5)}`;
}

export const MASCARA_CEP: Mascara = {
  limpar: (bruto) => bruto.replace(/\D/g, ''),
  formatar: formatarCep,
  maxLimpo: 8,
};
