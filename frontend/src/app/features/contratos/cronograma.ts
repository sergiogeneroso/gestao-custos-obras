/**
 * Matemática do cronograma de parcelas (ADR-037), extraída de `ContratoFormDialog` para poder
 * testar sem TestBed. Aritmética em centavos inteiros: cada função converte reais em centavos na
 * entrada (`Math.round(valor * 100)`) e devolve reais (centavos / 100) — evita erro de ponto
 * flutuante ao somar e ratear dinheiro.
 */

function paraCentavos(valor: number): number {
  return Math.round(valor * 100);
}

function paraReais(centavos: number): number {
  return centavos / 100;
}

/** Entrada + soma das parcelas — valor de referência do preço do lote (ADR-037). */
export function totalCronograma(entrada: number | null, valoresParcelas: number[]): number {
  const centavos =
    paraCentavos(entrada ?? 0) + valoresParcelas.reduce((soma, valor) => soma + paraCentavos(valor), 0);
  return paraReais(centavos);
}

/** Positivo = juros embutidos; zero = sem juros; negativo = preço informado maior que o total. */
export function diferencaJuros(total: number, precoAVista: number | null): number {
  if (precoAVista == null) {
    return 0;
  }
  return paraReais(paraCentavos(total) - paraCentavos(precoAVista));
}

/**
 * Rateia a diferença de juros proporcionalmente ao valor de cada parcela, com a sobra do
 * arredondamento na última. `null` quando não há nada a ratear (diferença zero/negativa ou sem
 * parcelas) — o chamador não deve mexer nos valores de juros já digitados nesse caso.
 */
export function distribuirJuros(diferenca: number, valoresParcelas: number[]): number[] | null {
  const totalCentavos = paraCentavos(diferenca);
  const centavosParcelas = valoresParcelas.map(paraCentavos);
  const somaCentavos = centavosParcelas.reduce((soma, valor) => soma + valor, 0);
  if (totalCentavos <= 0 || somaCentavos <= 0) {
    return null;
  }

  let alocado = 0;
  return centavosParcelas.map((valor, indice) => {
    const ultima = indice === centavosParcelas.length - 1;
    const juros = ultima ? totalCentavos - alocado : Math.round((totalCentavos * valor) / somaCentavos);
    alocado += juros;
    return paraReais(juros);
  });
}

/** Vencimento dia 31 num mês de 30 (ou fevereiro) cai no último dia do mês, em vez de pular pro seguinte. */
export function somarMeses(base: Date, meses: number): Date {
  const alvo = new Date(base.getFullYear(), base.getMonth() + meses, 1);
  const ultimoDia = new Date(alvo.getFullYear(), alvo.getMonth() + 1, 0).getDate();
  alvo.setDate(Math.min(base.getDate(), ultimoDia));
  return alvo;
}

export interface ParcelaGerada {
  numero: number;
  dataVencimento: Date;
  valor: number;
}

/** N parcelas mensais a partir de `numeroInicial`, um mês de intervalo entre vencimentos. */
export function gerarParcelas(
  numeroInicial: number,
  quantidade: number,
  valorParcela: number,
  primeiroVencimento: Date,
): ParcelaGerada[] {
  const valor = paraReais(paraCentavos(valorParcela));
  return Array.from({ length: quantidade }, (_, i) => ({
    numero: numeroInicial + i,
    dataVencimento: somarMeses(primeiroVencimento, i),
    valor,
  }));
}

/** Numeração continua do maior número já usado nas parcelas existentes. */
export function proximoNumero(numeros: number[]): number {
  return numeros.length ? Math.max(...numeros) + 1 : 1;
}
