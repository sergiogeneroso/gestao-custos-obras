import { FaseImovel } from '../imoveis/imovel.model';

export type TipoAnexoDespesa = 'COMPROVANTE' | 'NOTA_FISCAL' | 'RECIBO' | 'CONTRATO' | 'OUTRO';

export const TIPO_ANEXO_DESPESA_LABEL: Record<TipoAnexoDespesa, string> = {
  COMPROVANTE: 'Comprovante',
  NOTA_FISCAL: 'Nota fiscal',
  RECIBO: 'Recibo',
  CONTRATO: 'Contrato',
  OUTRO: 'Outro',
};

export type EtapaConstrucao =
  | 'SERVICOS_PRELIMINARES'
  | 'FUNDACAO'
  | 'ESTRUTURA'
  | 'ALVENARIA'
  | 'COBERTURA'
  | 'INSTALACOES'
  | 'ESQUADRIAS'
  | 'REVESTIMENTO'
  | 'PINTURA'
  | 'ACABAMENTO'
  | 'AREA_EXTERNA'
  | 'OUTRO';

// Ordem da lista = ordem cronológica da obra, que é como o usuário procura a etapa.
export const ETAPAS_CONSTRUCAO: EtapaConstrucao[] = [
  'SERVICOS_PRELIMINARES',
  'FUNDACAO',
  'ESTRUTURA',
  'ALVENARIA',
  'COBERTURA',
  'INSTALACOES',
  'ESQUADRIAS',
  'REVESTIMENTO',
  'PINTURA',
  'ACABAMENTO',
  'AREA_EXTERNA',
  'OUTRO',
];

export const ETAPA_CONSTRUCAO_LABEL: Record<EtapaConstrucao, string> = {
  SERVICOS_PRELIMINARES: 'Serviços preliminares',
  FUNDACAO: 'Fundação',
  ESTRUTURA: 'Estrutura',
  ALVENARIA: 'Alvenaria',
  COBERTURA: 'Cobertura',
  INSTALACOES: 'Instalações',
  ESQUADRIAS: 'Esquadrias',
  REVESTIMENTO: 'Revestimento',
  PINTURA: 'Pintura',
  ACABAMENTO: 'Acabamento',
  AREA_EXTERNA: 'Área externa',
  OUTRO: 'Outro',
};

export interface DespesaRequestDTO {
  imovelId: number | null;
  categoriaDespesaId: number;
  pagadorId: number;
  beneficiarioId: number | null;
  contratoFinanceiroId: number | null;
  faseImovel: FaseImovel | null;
  etapaConstrucao: EtapaConstrucao | null;
  valor: number;
  dataPagamento: string;
  descricao: string | null;
}

export interface DespesaResponseDTO {
  id: number;
  imovelId: number | null;
  imovelIdentificador: string | null;
  categoriaDespesaId: number;
  categoriaDespesaNome: string;
  pagadorId: number;
  pagadorNome: string;
  beneficiarioId: number | null;
  beneficiarioNome: string | null;
  contratoFinanceiroId: number | null;
  faseImovel: FaseImovel | null;
  etapaConstrucao: EtapaConstrucao | null;
  valor: number;
  dataPagamento: string;
  descricao: string | null;
  ativo: boolean;
}

export interface DespesaAnexoResponseDTO {
  id: number;
  despesaId: number;
  tipoAnexo: TipoAnexoDespesa;
  url: string;
  dataUpload: string;
}
