/** Siglas das 27 unidades da federação, para `mat-select` de UF em qualquer endereço. */
export const UFS = [
  'AC', 'AL', 'AM', 'AP', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MG', 'MS', 'MT', 'PA',
  'PB', 'PE', 'PI', 'PR', 'RJ', 'RN', 'RO', 'RR', 'RS', 'SC', 'SE', 'SP', 'TO',
] as const;

/**
 * Onde o negócio opera. Sugestão só para registro **novo**: preencher um registro
 * antigo sem UF faria o próximo "Salvar" gravar um estado que ninguém afirmou.
 */
export const UF_PADRAO = 'MG';
