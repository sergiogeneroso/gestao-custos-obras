import { describe, expect, it } from 'vitest';
import { cnpjValido, cpfValido, formatarCnpj, formatarCpf, formatarDocumento } from './documento';
import { formatarTelefone } from './telefone';

// O mesmo algoritmo roda no backend (shared/validacao/Documentos.java). Os casos aqui
// são os mesmos do DocumentosTest de lá, justamente para as duas pontas não divergirem.
describe('documento', () => {
  it('aceita CPF válido com ou sem pontuação', () => {
    expect(cpfValido('529.982.247-25')).toBe(true);
    expect(cpfValido('52998224725')).toBe(true);
  });

  it('recusa CPF com dígito errado, tamanho errado ou todo igual', () => {
    expect(cpfValido('529.982.247-26')).toBe(false);
    expect(cpfValido('5299822472')).toBe(false);
    expect(cpfValido('111.111.111-11')).toBe(false);
  });

  it('aceita CNPJ numérico e alfanumérico válidos', () => {
    expect(cnpjValido('11.222.333/0001-81')).toBe(true);
    expect(cnpjValido('12.ABC.345/01DE-35')).toBe(true);
  });

  it('recusa CNPJ com dígito errado ou todo igual', () => {
    expect(cnpjValido('11.222.333/0001-82')).toBe(false);
    expect(cnpjValido('00000000000000')).toBe(false);
  });

  it('formata para exibição a partir do valor gravado, que vem sem pontuação', () => {
    expect(formatarCpf('52998224725')).toBe('529.982.247-25');
    expect(formatarCnpj('11222333000181')).toBe('11.222.333/0001-81');
    expect(formatarDocumento('52998224725', 'FISICA')).toBe('529.982.247-25');
    expect(formatarDocumento('11222333000181', 'JURIDICA')).toBe('11.222.333/0001-81');
  });

  it('formata parcialmente enquanto o valor ainda está incompleto', () => {
    expect(formatarCpf('529982')).toBe('529.982');
    expect(formatarCnpj('11222')).toBe('11.222');
  });
});

describe('telefone', () => {
  it('quebra celular em 5+4 e fixo em 4+4', () => {
    expect(formatarTelefone('11987654321')).toBe('(11) 98765-4321');
    expect(formatarTelefone('1132654321')).toBe('(11) 3265-4321');
  });

  it('formata parcialmente e trata vazio', () => {
    expect(formatarTelefone('119876')).toBe('(11) 9876');
    expect(formatarTelefone(null)).toBe('');
  });
});
