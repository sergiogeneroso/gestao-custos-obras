export interface CategoriaDespesaRequestDTO {
  nome: string;
  descricao: string | null;
}

export interface CategoriaDespesaResponseDTO {
  id: number;
  nome: string;
  descricao: string | null;
}
