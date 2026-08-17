export interface FornecedorRequestDTO {
  pessoaId: number;
  areaAtuacao: string | null;
  observacoes: string | null;
}

export interface FornecedorResponseDTO {
  id: number;
  pessoaId: number;
  pessoaNome: string;
  areaAtuacao: string | null;
  observacoes: string | null;
  ativo: boolean;
}
