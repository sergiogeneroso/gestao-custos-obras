export type TipoContratoFinanceiro = 'PARCELAMENTO_COMPRA' | 'FINANCIAMENTO_CONSTRUCAO' | 'PARCELAMENTO_VENDA';
export type SituacaoContrato = 'ATIVO' | 'QUITADO';

export const TIPO_CONTRATO_LABEL: Record<TipoContratoFinanceiro, string> = {
  PARCELAMENTO_COMPRA: 'Parcelamento da compra',
  FINANCIAMENTO_CONSTRUCAO: 'Financiamento da construção',
  PARCELAMENTO_VENDA: 'Parcelamento da venda',
};

export const SITUACAO_CONTRATO_LABEL: Record<SituacaoContrato, string> = {
  ATIVO: 'Ativo',
  QUITADO: 'Quitado',
};

export interface ParcelaContratoRequestDTO {
  numero: number;
  dataVencimento: string;
  valor: number;
  valorJuros: number | null;
}

export interface ParcelaContratoResponseDTO {
  id: number;
  numero: number;
  dataVencimento: string;
  valor: number;
  valorJuros: number | null;
  dataPagamento: string | null;
  valorPago: number | null;
}

export interface ContratoFinanceiroRequestDTO {
  imovelId: number;
  tipo: TipoContratoFinanceiro;
  contraparteId: number;
  valorContratado: number;
  parcelas: ParcelaContratoRequestDTO[] | null;
}

export interface ContratoFinanceiroResponseDTO {
  id: number;
  imovelId: number;
  imovelIdentificador: string;
  tipo: TipoContratoFinanceiro;
  contraparteId: number;
  contraparteNome: string;
  valorContratado: number;
  situacao: SituacaoContrato;
  dataQuitacao: string | null;
  valorQuitacao: number | null;
  parcelas: ParcelaContratoResponseDTO[];
}

export interface ContratoQuitacaoRequestDTO {
  dataQuitacao: string;
  valorQuitacao: number;
}

export interface ParcelaPagamentoRequestDTO {
  dataPagamento: string;
  valorPago: number;
}
