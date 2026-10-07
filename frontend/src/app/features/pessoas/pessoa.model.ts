import { OpcaoAutocomplete } from '../../shared/autocomplete/autocomplete';

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

// Busca por nome, CPF/CNPJ e área de atuação ("pedreiro" encontra o João); a área aparece ao lado
// do nome só para fornecedor.
export function opcaoPessoa(pessoa: PessoaResponseDTO): OpcaoAutocomplete {
  return {
    id: pessoa.id,
    rotulo: pessoa.nome,
    detalhe: pessoa.fornecedor ? pessoa.areaAtuacao : null,
    termos: pessoa.documento,
    inativo: !pessoa.ativo,
  };
}
