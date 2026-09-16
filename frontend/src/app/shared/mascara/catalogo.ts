import { MASCARA_CEP } from './cep';
import { MASCARA_CNO } from './cno';
import { Mascara } from './mascara.directive';
import { MASCARA_TELEFONE } from './telefone';

/**
 * Máscaras fixas por nome, para o template pedir `appMascara="cep"` sem importar
 * nada. Máscara nova de campo que se repete entre telas entra aqui.
 */
export const MASCARAS = {
  telefone: MASCARA_TELEFONE,
  cep: MASCARA_CEP,
  cno: MASCARA_CNO,
} satisfies Record<string, Mascara>;

export type NomeMascara = keyof typeof MASCARAS;
