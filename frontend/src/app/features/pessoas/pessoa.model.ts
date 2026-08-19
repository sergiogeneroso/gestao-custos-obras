export type TipoPessoa = 'FISICA' | 'JURIDICA';

export const TIPO_PESSOA_LABEL: Record<TipoPessoa, string> = {
  FISICA: 'Pessoa física',
  JURIDICA: 'Pessoa jurídica',
};

export interface PessoaRequestDTO {
  nome: string;
  tipoPessoa: TipoPessoa;
  documento: string;
  email: string | null;
  telefone: string | null;
  fornecedor: boolean;
  areaAtuacao: string | null;
  observacoes: string | null;
}

export interface PessoaResponseDTO {
  id: number;
  nome: string;
  tipoPessoa: TipoPessoa;
  documento: string;
  email: string | null;
  telefone: string | null;
  fornecedor: boolean;
  areaAtuacao: string | null;
  observacoes: string | null;
  ativo: boolean;
}
