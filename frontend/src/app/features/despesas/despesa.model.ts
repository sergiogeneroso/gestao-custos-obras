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
  observacao: string | null;
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
  observacao: string | null;
  ativo: boolean;
  /**
   * Quantos anexos a despesa tem. Só a busca paginada preenche; nos outros endpoints vem nulo,
   * que significa "não calculado" e nunca "sem anexo" — a tela não pode desenhar alerta de
   * comprovante faltando a partir de nulo.
   */
  quantidadeAnexos: number | null;
}

export interface DespesaAnexoResponseDTO {
  id: number;
  despesaId: number;
  tipoAnexo: TipoAnexoDespesa;
  url: string;
  dataUpload: string;
}

const EXTENSOES_IMAGEM = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg']);

/** Deduz o formato do anexo pela extensão da URL, pra decidir como fazer a pré-visualização. */
export function tipoArquivoAnexo(url: string): 'imagem' | 'pdf' | 'outro' {
  const extensao = url.split('.').pop()?.toLowerCase() ?? '';
  if (extensao === 'pdf') {
    return 'pdf';
  }
  return EXTENSOES_IMAGEM.has(extensao) ? 'imagem' : 'outro';
}
