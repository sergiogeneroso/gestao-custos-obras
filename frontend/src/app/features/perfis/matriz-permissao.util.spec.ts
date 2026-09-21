import { describe, expect, it } from 'vitest';
import { alternarPermissao, matrizDePermissoes, permissoesDaMatriz } from './matriz-permissao.util';

// ADR-046: "acessar" é pré-requisito das outras três ações no mesmo domínio — a tela não deve
// deixar montar uma combinação que o backend recusaria ao salvar.
describe('matriz-permissao.util', () => {
  it('marcar incluir liga acessar junto no mesmo domínio', () => {
    const matriz = matrizDePermissoes([]);

    const resultado = alternarPermissao(matriz, 'DESPESA', 'INCLUIR');

    expect(resultado.DESPESA.INCLUIR).toBe(true);
    expect(resultado.DESPESA.ACESSAR).toBe(true);
  });

  it('desmarcar acessar limpa as outras três ações do mesmo domínio', () => {
    const matriz = matrizDePermissoes([
      { dominio: 'DESPESA', acao: 'ACESSAR' },
      { dominio: 'DESPESA', acao: 'INCLUIR' },
      { dominio: 'DESPESA', acao: 'ALTERAR' },
    ]);

    const resultado = alternarPermissao(matriz, 'DESPESA', 'ACESSAR');

    expect(resultado.DESPESA.ACESSAR).toBe(false);
    expect(resultado.DESPESA.INCLUIR).toBe(false);
    expect(resultado.DESPESA.ALTERAR).toBe(false);
  });

  it('alternar em um domínio não afeta outro domínio', () => {
    const matriz = matrizDePermissoes([{ dominio: 'PESSOA', acao: 'ACESSAR' }]);

    const resultado = alternarPermissao(matriz, 'DESPESA', 'ACESSAR');

    expect(resultado.PESSOA.ACESSAR).toBe(true);
    expect(resultado.DESPESA.ACESSAR).toBe(true);
  });

  it('permissoesDaMatriz converte só as ações marcadas, de volta e forma', () => {
    const matriz = matrizDePermissoes([
      { dominio: 'DESPESA', acao: 'ACESSAR' },
      { dominio: 'DESPESA', acao: 'INCLUIR' },
      { dominio: 'IMOVEL', acao: 'ACESSAR' },
    ]);

    const resultado = permissoesDaMatriz(matriz);

    expect(resultado).toHaveLength(3);
    expect(resultado).toEqual(
      expect.arrayContaining([
        { dominio: 'DESPESA', acao: 'ACESSAR' },
        { dominio: 'DESPESA', acao: 'INCLUIR' },
        { dominio: 'IMOVEL', acao: 'ACESSAR' },
      ]),
    );
  });
});
