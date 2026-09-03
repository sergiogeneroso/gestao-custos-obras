import { describe, expect, it } from 'vitest';
import { tipoArquivoAnexo } from './despesa.model';

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
