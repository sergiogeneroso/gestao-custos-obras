import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extrai o motivo real de um erro da API. O backend sempre manda o motivo em
 * `mensagem` (ver ApiErrorHandler); o texto `padrao` só entra quando não há
 * resposta nenhuma para ler.
 *
 * Substitui os `erro.error?.mensagem ?? '<texto genérico>'` espalhados pelos
 * diálogos, que engoliam o motivo sempre que a resposta não tinha esse campo.
 */
export function mensagemErro(erro: HttpErrorResponse, padrao: string): string {
  // status 0 = requisição nem chegou ao servidor. `mensagem` aqui seria a do
  // navegador ("Http failure response for..."), que não ajuda ninguém.
  if (erro.status === 0) {
    return `${padrao} O servidor não respondeu — verifique se o backend está no ar.`;
  }

  const corpo = erro.error;
  if (typeof corpo === 'string' && corpo.trim()) {
    return corpo;
  }

  const mensagem = corpo?.mensagem;
  if (typeof mensagem === 'string' && mensagem.trim()) {
    return mensagem;
  }

  return padrao;
}
