import { describe, expect, it } from 'vitest';
import { ContratoFinanceiroResponseDTO, ParcelaContratoResponseDTO, saldoAEstornar } from './contrato.model';

describe('saldoAEstornar', () => {
  it('soma o pago nas parcelas quitadas, menos o que já foi devolvido', () => {
    const c = contrato([parcelaPaga(1000), parcelaPaga(500)], 300);
    expect(saldoAEstornar(c)).toBe(1200); // 1000 + 500 - 300
  });

  it('ignora parcelas ainda não pagas', () => {
    const c = contrato([parcelaPaga(1000), parcelaNaoPaga(2000)], 0);
    expect(saldoAEstornar(c)).toBe(1000);
  });

  it('não perde centavo por ponto flutuante ao somar os pagamentos', () => {
    const c = contrato([parcelaPaga(33.33), parcelaPaga(33.33), parcelaPaga(33.34)], 0.1);
    expect(saldoAEstornar(c)).toBe(99.9); // 100,00 - 0,10
  });
});

function parcelaPaga(valorPago: number): ParcelaContratoResponseDTO {
  return {
    id: 1,
    numero: 1,
    dataVencimento: '2026-01-10',
    valor: valorPago,
    valorJuros: null,
    dataPagamento: '2026-01-10',
    valorPago,
  };
}

function parcelaNaoPaga(valor: number): ParcelaContratoResponseDTO {
  return {
    id: 2,
    numero: 2,
    dataVencimento: '2026-02-10',
    valor,
    valorJuros: null,
    dataPagamento: null,
    valorPago: null,
  };
}

function contrato(parcelas: ParcelaContratoResponseDTO[], valorEstornado: number): ContratoFinanceiroResponseDTO {
  return {
    id: 1,
    imovelId: 1,
    imovelIdentificador: 'Lote 1',
    tipo: 'PARCELAMENTO_VENDA',
    contraparteId: 1,
    contraparteNome: 'Comprador',
    valorContratado: 100000,
    situacao: 'CANCELADO',
    dataQuitacao: null,
    valorQuitacao: null,
    dataCancelamento: '2026-03-01',
    motivoCancelamento: 'Venda desfeita',
    valorEstornado,
    dataEstorno: null,
    parcelas,
  };
}
