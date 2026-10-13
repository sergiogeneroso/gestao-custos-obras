import { IdOpcao } from '../../shared/autocomplete/autocomplete';
import { PaginaDTO } from '../../shared/pagina/pagina.model';
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
   * Quantos anexos a despesa tem, de qualquer tipo. Só a busca paginada preenche; nos outros
   * endpoints vem nulo ("não calculado").
   */
  quantidadeAnexos: number | null;
  /**
   * Se a despesa tem ao menos um anexo do tipo COMPROVANTE (a prova de pagamento — RECIBO,
   * NOTA_FISCAL etc. não contam). Só a busca paginada preenche; nulo é "não calculado", nunca
   * "sem comprovante".
   */
  temComprovante: boolean | null;
  /** Quando os associados conferiram a despesa; nulo = pendente (ADR-048). Conferida trava editar/excluir. */
  conferidaEm: string | null;
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

/**
 * Filtros da listagem de despesas, todos combinados com "e". `imovel`: vazio = todos, 'IMOVEL' =
 * qualquer imóvel, 'GERAL' = gastos gerais (sem imóvel), número = aquele imóvel.
 */
export interface FiltroDespesas {
  imovel: IdOpcao | null;
  categoriaDespesaId: number | null;
  dataInicio: Date | null;
  dataFim: Date | null;
  pagadorId: number | null;
  beneficiarioId: number | null;
  faseImovel: FaseImovel | null;
  etapaConstrucao: EtapaConstrucao | null;
  semComprovante: boolean;
  /** true = só conferidas, false = só pendentes, null = todas (ADR-048). */
  conferida: boolean | null;
}

export const FILTRO_DESPESAS_VAZIO: FiltroDespesas = {
  imovel: null,
  categoriaDespesaId: null,
  dataInicio: null,
  dataFim: null,
  pagadorId: null,
  beneficiarioId: null,
  faseImovel: null,
  etapaConstrucao: null,
  semComprovante: false,
  conferida: null,
};

/**
 * Nunca deixa uma combinação que sempre volta vazia: gasto geral não tem fase nem etapa, e etapa
 * só existe na fase Construção (mesma regra do formulário de despesa).
 */
export function ajustarFiltro(filtro: FiltroDespesas): FiltroDespesas {
  const ajustado = { ...filtro };
  if (ajustado.imovel === 'GERAL') {
    ajustado.faseImovel = null;
  }
  if (ajustado.faseImovel !== 'CONSTRUCAO') {
    ajustado.etapaConstrucao = null;
  }
  return ajustado;
}

// Os filtros que ficam dentro de "Mais filtros", para o contador do botão.
export function contarMaisFiltros(filtro: FiltroDespesas): number {
  return [filtro.pagadorId, filtro.beneficiarioId, filtro.faseImovel, filtro.etapaConstrucao]
    .filter((v) => v !== null).length + (filtro.semComprovante ? 1 : 0);
}

// valorTotal soma todas as despesas que casam com o filtro, não só as da página.
export interface BuscaDespesaDTO {
  pagina: PaginaDTO<DespesaResponseDTO>;
  valorTotal: number;
}
