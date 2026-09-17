// Mesmo limite do backend (spring.servlet.multipart.max-file-size) — checar aqui evita subir um
// arquivo grande inteiro só para descobrir, na resposta, que ele passou do limite.
export const TAMANHO_MAXIMO_ARQUIVO_BYTES = 20 * 1024 * 1024;

export function arquivoDentroDoLimite(arquivo: File): boolean {
  return arquivo.size <= TAMANHO_MAXIMO_ARQUIVO_BYTES;
}

export const MENSAGEM_ARQUIVO_GRANDE = 'Arquivo maior que o limite permitido de 20MB.';
