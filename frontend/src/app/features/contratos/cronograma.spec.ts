import { describe, expect, it } from 'vitest';
import { diferencaJuros, distribuirJuros, gerarParcelas, proximoNumero, somarMeses, totalCronograma } from './cronograma';

describe('totalCronograma', () => {
  it('soma entrada e parcelas sem perder centavo em ponto flutuante clássico', () => {
    // 10,10 + 20,20 + 30,30 daria 60,599999999999994 em ponto flutuante puro.
    expect(totalCronograma(10.1, [20.2, 30.3])).toBe(60.6);
  });
});

describe('diferencaJuros', () => {
  it('fecha exata em zero quando entrada + parcelas cobre o preço à vista', () => {
    expect(diferencaJuros(100, 100)).toBe(0);
  });

  it('é negativa quando o preço informado supera o total do cronograma', () => {
    expect(diferencaJuros(90, 100)).toBe(-10);
  });
});

describe('distribuirJuros', () => {
  it('rateia 100,00 em 3 parcelas iguais sem perder centavo, com a sobra na última', () => {
    expect(distribuirJuros(100, [100, 100, 100])).toEqual([33.33, 33.33, 33.34]);
  });

  it('não rateia (retorna null) quando não há diferença de juros a distribuir', () => {
    expect(distribuirJuros(0, [100, 100])).toBeNull();
  });
});

describe('somarMeses', () => {
  it('dia 31 cai no último dia de fevereiro num ano comum', () => {
    const resultado = somarMeses(new Date(2026, 0, 31), 1);
    expect(resultado).toEqual(new Date(2026, 1, 28));
  });

  it('dia 31 cai em 29 de fevereiro num ano bissexto', () => {
    const resultado = somarMeses(new Date(2028, 0, 31), 1);
    expect(resultado).toEqual(new Date(2028, 1, 29));
  });
});

describe('gerarParcelas', () => {
  it('gera N parcelas mensais numeradas a partir do número inicial', () => {
    expect(gerarParcelas(3, 2, 100, new Date(2026, 0, 10))).toEqual([
      { numero: 3, dataVencimento: new Date(2026, 0, 10), valor: 100 },
      { numero: 4, dataVencimento: new Date(2026, 1, 10), valor: 100 },
    ]);
  });
});

describe('proximoNumero', () => {
  it('continua do maior número já usado', () => {
    expect(proximoNumero([1, 2, 5])).toBe(6);
  });

  it('começa em 1 quando não há parcela nenhuma', () => {
    expect(proximoNumero([])).toBe(1);
  });
});
