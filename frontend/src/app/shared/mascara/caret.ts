import { Mascara } from './mascara.directive';

/** Letra ou dígito: o que a máscara grava. Todo o resto é pontuação que ela mesma põe. */
const DADO = /[A-Za-z0-9]/;

export type Apagou = 'tras' | 'frente' | null;

export interface Edicao {
  limpo: string;
  exibido: string;
  caret: number;
}

/**
 * Reformata o input a cada tecla sem o cursor pular para o fim.
 *
 * A pontuação muda de lugar conforme o valor cresce, então a posição do cursor em
 * caracteres não sobrevive à reformatação. O que sobrevive é quantos caracteres de
 * dado existem antes dele (ou depois, na moeda, que cresce da direita): conta-se
 * isso no texto digitado e recoloca-se o cursor no mesmo ponto do texto formatado.
 */
export function reformatar(
  texto: string,
  caret: number,
  anterior: string,
  mascara: Mascara,
  apagou: Apagou,
): Edicao {
  if (apagou && contarDados(texto) === contarDados(anterior)) {
    // Apagou só pontuação: sem isto ela volta na reformatação e o Backspace fica
    // preso nela. Apaga o dado vizinho, que é o que o usuário quis dizer.
    const alvo = apagou === 'tras' ? ultimoDadoAntes(texto, caret) : primeiroDadoDesde(texto, caret);
    if (alvo >= 0) {
      texto = texto.slice(0, alvo) + texto.slice(alvo + 1);
      caret = Math.min(caret, alvo);
    }
  }

  const limpo = mascara.limpar(texto).slice(0, mascara.maxLimpo);
  const exibido = mascara.formatar(limpo);
  const posicao = mascara.ancorarNoFim
    ? posicaoPelaDireita(exibido, contarDados(texto.slice(caret)))
    : posicaoPelaEsquerda(exibido, contarDados(texto.slice(0, caret)));
  return { limpo, exibido, caret: posicao };
}

/** Aplica a edição do evento `input` no próprio elemento e devolve o valor limpo. */
export function reformatarInput(input: HTMLInputElement, evento: Event, anterior: string, mascara: Mascara): Edicao {
  const tipo = (evento as InputEvent).inputType;
  const apagou: Apagou = tipo === 'deleteContentBackward' ? 'tras' : tipo === 'deleteContentForward' ? 'frente' : null;
  const edicao = reformatar(input.value, input.selectionStart ?? input.value.length, anterior, mascara, apagou);
  input.value = edicao.exibido;
  // Só mexe no cursor com foco: setSelectionRange num campo sem foco rouba o foco no Safari.
  if (document.activeElement === input) {
    input.setSelectionRange(edicao.caret, edicao.caret);
  }
  return edicao;
}

function contarDados(texto: string): number {
  let total = 0;
  for (const c of texto) if (DADO.test(c)) total++;
  return total;
}

function posicaoPelaEsquerda(exibido: string, dadosAntes: number): number {
  if (dadosAntes === 0) return 0;
  let contados = 0;
  for (let i = 0; i < exibido.length; i++) {
    if (DADO.test(exibido[i]) && ++contados === dadosAntes) return i + 1;
  }
  return exibido.length;
}

function posicaoPelaDireita(exibido: string, dadosDepois: number): number {
  if (dadosDepois === 0) return exibido.length;
  let contados = 0;
  for (let i = exibido.length - 1; i >= 0; i--) {
    if (DADO.test(exibido[i]) && ++contados === dadosDepois) return i;
  }
  return 0;
}

function ultimoDadoAntes(texto: string, caret: number): number {
  for (let i = caret - 1; i >= 0; i--) if (DADO.test(texto[i])) return i;
  return -1;
}

function primeiroDadoDesde(texto: string, caret: number): number {
  for (let i = caret; i < texto.length; i++) if (DADO.test(texto[i])) return i;
  return -1;
}
