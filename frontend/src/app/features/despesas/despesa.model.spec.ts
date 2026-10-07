import { describe, expect, it } from 'vitest';
import { ajustarFiltro, contarMaisFiltros, FILTRO_DESPESAS_VAZIO, tipoArquivoAnexo } from './despesa.model';

describe('tipoArquivoAnexo', () => {
  it('reconhece imagem pela extensão', () => {
    expect(tipoArquivoAnexo('/api/arquivos/download/despesas/1/nota.jpg')).toBe('imagem');
    expect(tipoArquivoAnexo('/api/arquivos/download/despesas/1/nota.PNG')).toBe('imagem');
  });

  it('reconhece pdf pela extensão', () => {
    expect(tipoArquivoAnexo('/api/arquivos/download/despesas/1/nota.pdf')).toBe('pdf');
  });

  it('cai em "outro" para extensão desconhecida ou ausente', () => {
    expect(tipoArquivoAnexo('/api/arquivos/download/despesas/1/nota.docx')).toBe('outro');
    expect(tipoArquivoAnexo('/api/arquivos/download/despesas/1/nota')).toBe('outro');
  });
});

describe('ajustarFiltro', () => {
  it('gasto geral limpa fase e etapa', () => {
    const filtro = ajustarFiltro({
      ...FILTRO_DESPESAS_VAZIO,
      imovel: 'GERAL',
      faseImovel: 'CONSTRUCAO',
      etapaConstrucao: 'FUNDACAO',
    });

    expect(filtro.faseImovel).toBeNull();
    expect(filtro.etapaConstrucao).toBeNull();
  });

  it('etapa só sobrevive na fase Construção', () => {
    const base = { ...FILTRO_DESPESAS_VAZIO, imovel: 10, etapaConstrucao: 'FUNDACAO' as const };

    expect(ajustarFiltro({ ...base, faseImovel: 'CONSTRUCAO' }).etapaConstrucao).toBe('FUNDACAO');
    expect(ajustarFiltro({ ...base, faseImovel: 'LOTE' }).etapaConstrucao).toBeNull();
  });

  it('conta só os filtros de "Mais filtros"', () => {
    expect(
      contarMaisFiltros({ ...FILTRO_DESPESAS_VAZIO, imovel: 10, categoriaDespesaId: 2, pagadorId: 5, semComprovante: true }),
    ).toBe(2);
  });
});
