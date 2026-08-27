import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { TipoPessoa } from '../../features/pessoas/pessoa.model';
import { Mascara } from './mascara.directive';

/**
 * CPF/CNPJ: máscara de exibição e dígito verificador.
 *
 * O algoritmo está duplicado com o backend (shared/validacao/Documentos.java) de
 * propósito: a fronteira de confiança é o servidor, que continua sendo quem
 * recusa o documento inválido; isto aqui existe só para o formulário reclamar na
 * hora, sem ida e volta ao servidor.
 */

export function limparDocumento(bruto: string): string {
  return bruto.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
}

export function formatarCpf(digitos: string): string {
  const d = digitos.slice(0, 11);
  if (d.length <= 3) return d;
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`;
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`;
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
}

export function formatarCnpj(caracteres: string): string {
  const c = caracteres.slice(0, 14);
  if (c.length <= 2) return c;
  if (c.length <= 5) return `${c.slice(0, 2)}.${c.slice(2)}`;
  if (c.length <= 8) return `${c.slice(0, 2)}.${c.slice(2, 5)}.${c.slice(5)}`;
  if (c.length <= 12) return `${c.slice(0, 2)}.${c.slice(2, 5)}.${c.slice(5, 8)}/${c.slice(8)}`;
  return `${c.slice(0, 2)}.${c.slice(2, 5)}.${c.slice(5, 8)}/${c.slice(8, 12)}-${c.slice(12)}`;
}

export function formatarDocumento(documento: string | null | undefined, tipo: TipoPessoa): string {
  if (!documento) return '';
  const limpo = limparDocumento(documento);
  return tipo === 'FISICA' ? formatarCpf(limpo) : formatarCnpj(limpo);
}

export function mascaraDocumento(tipo: TipoPessoa): Mascara {
  return tipo === 'FISICA'
    ? { limpar: limparDocumento, formatar: formatarCpf, maxLimpo: 11 }
    : { limpar: limparDocumento, formatar: formatarCnpj, maxLimpo: 14 };
}

export function cpfValido(documento: string): boolean {
  const cpf = limparDocumento(documento);
  if (cpf.length !== 11 || !/^\d{11}$/.test(cpf) || todosIguais(cpf)) {
    return false;
  }
  const digito = (pesos: number[]) => {
    const soma = pesos.reduce((acc, peso, i) => acc + Number(cpf[i]) * peso, 0);
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  return digito([10, 9, 8, 7, 6, 5, 4, 3, 2]) === Number(cpf[9])
    && digito([11, 10, 9, 8, 7, 6, 5, 4, 3, 2]) === Number(cpf[10]);
}

/**
 * Aceita o CNPJ numérico de sempre e o alfanumérico emitido a partir de 2026: o
 * algoritmo é o mesmo, com cada caractere valendo `código - 48` — que devolve o
 * próprio dígito quando ele é numérico. Os dois verificadores seguem numéricos.
 */
export function cnpjValido(documento: string): boolean {
  const cnpj = limparDocumento(documento);
  if (cnpj.length !== 14 || !/^[A-Z0-9]{12}\d{2}$/.test(cnpj) || todosIguais(cnpj)) {
    return false;
  }
  const digito = (pesos: number[]) => {
    const soma = pesos.reduce((acc, peso, i) => acc + (cnpj.charCodeAt(i) - 48) * peso, 0);
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  return digito([5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]) === Number(cnpj[12])
    && digito([6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]) === Number(cnpj[13]);
}

/** Validador do formulário. Campo vazio fica para o `Validators.required`. */
export function documentoValidator(tipo: () => TipoPessoa): ValidatorFn {
  return (controle: AbstractControl): ValidationErrors | null => {
    const valor = controle.value as string | null;
    if (!valor) {
      return null;
    }
    const valido = tipo() === 'FISICA' ? cpfValido(valor) : cnpjValido(valor);
    return valido ? null : { documento: true };
  };
}

// 111.111.111-11 passa em todos os verificadores e não é documento de ninguém.
function todosIguais(valor: string): boolean {
  return new Set(valor).size === 1;
}
