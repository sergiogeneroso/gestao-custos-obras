import { describe, expect, it } from 'vitest';
import { MASCARA_MOEDA } from '../moeda/moeda.directive';
import { reformatar } from './caret';
import { mascaraDocumento } from './documento';

const CPF = mascaraDocumento('FISICA');

describe('reformatar', () => {
  it('pontua enquanto digita, com o cursor no fim', () => {
    expect(reformatar('1234', 4, '123', CPF, null)).toEqual({ limpo: '1234', exibido: '123.4', caret: 5 });
  });

  it('mantém o cursor no mesmo dado ao digitar no meio', () => {
    // "123.|456" + "9" → dado 9 inserido após o 3º dígito, cursor logo depois dele.
    expect(reformatar('123.9456', 5, '123.456', CPF, null)).toEqual({ limpo: '1239456', exibido: '123.945.6', caret: 5 });
  });

  it('Backspace sobre a pontuação apaga o dígito anterior em vez de travar', () => {
    // "123.|456" com Backspace: o navegador tirou só o ponto.
    expect(reformatar('123456', 3, '123.456', CPF, 'tras')).toEqual({ limpo: '12456', exibido: '124.56', caret: 2 });
  });

  it('Delete sobre a pontuação apaga o dígito seguinte', () => {
    expect(reformatar('123456', 3, '123.456', CPF, 'frente')).toEqual({ limpo: '12356', exibido: '123.56', caret: 3 });
  });

  it('descarta o que passa do tamanho máximo', () => {
    expect(reformatar('529.982.247-259', 15, '529.982.247-25', CPF, null).exibido).toBe('529.982.247-25');
  });

  describe('moeda em caixa registradora', () => {
    it('os dois últimos dígitos são os centavos', () => {
      expect(reformatar('12345', 5, '1.234', MASCARA_MOEDA, null)).toEqual({ limpo: '12345', exibido: '123,45', caret: 6 });
      expect(reformatar('5', 1, '', MASCARA_MOEDA, null).exibido).toBe('0,05');
    });

    it('apagar o último dígito volta uma casa, e apagar tudo esvazia', () => {
      expect(reformatar('123,4', 5, '123,45', MASCARA_MOEDA, 'tras').exibido).toBe('12,34');
      expect(reformatar('0,0', 3, '0,05', MASCARA_MOEDA, 'tras')).toEqual({ limpo: '', exibido: '', caret: 0 });
    });

    it('ancora o cursor pela direita quando a pontuação muda', () => {
      // "1.234,|56" + Backspace tira a vírgula → apaga o 4; o cursor fica antes dos centavos.
      expect(reformatar('1.23456', 5, '1.234,56', MASCARA_MOEDA, 'tras')).toEqual({ limpo: '12356', exibido: '123,56', caret: 4 });
    });
  });
});
