import { Mascara } from './mascara.directive';
import { MASCARA_TELEFONE } from './telefone';

/**
 * Máscaras fixas por nome, para o template pedir `appMascara="telefone"` sem
 * importar nada. Máscara nova de campo que se repete entre telas entra aqui.
 */
export const MASCARAS = {
  telefone: MASCARA_TELEFONE,
} satisfies Record<string, Mascara>;

export type NomeMascara = keyof typeof MASCARAS;
