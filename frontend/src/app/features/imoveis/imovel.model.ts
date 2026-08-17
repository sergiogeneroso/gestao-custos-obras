export type FaseImovel = 'LOTE' | 'CONSTRUCAO' | 'CASA';
export type SituacaoImovel = 'ADQUIRIDO' | 'A_VENDA' | 'VENDIDO';

export const FASE_IMOVEL_LABEL: Record<FaseImovel, string> = {
  LOTE: 'Lote',
  CONSTRUCAO: 'Construção',
  CASA: 'Casa',
};

export const SITUACAO_IMOVEL_LABEL: Record<SituacaoImovel, string> = {
  ADQUIRIDO: 'Adquirido',
  A_VENDA: 'À venda',
  VENDIDO: 'Vendido',
};

// LOTE → CONSTRUCAO → CASA, nunca retrocede (.agents/rules/ciclo-vida-imovel.md).
export const PROXIMA_FASE: Record<FaseImovel, FaseImovel | null> = {
  LOTE: 'CONSTRUCAO',
  CONSTRUCAO: 'CASA',
  CASA: null,
};

export interface ImovelRequestDTO {
  identificador: string;
  endereco: string | null;
  area: number | null;
  dataInicioLote: string;
  dataInicioConstrucao: string | null;
  dataConclusaoObra: string | null;
  custoEstimadoObra: number | null;
  previsaoConclusao: string | null;
  compraValor: number | null;
  compraData: string | null;
  compraVendedorId: number | null;
  vendaValorPretendido: number | null;
  descricao: string | null;
}

export interface ImovelResponseDTO {
  id: number;
  identificador: string;
  fase: FaseImovel;
  situacao: SituacaoImovel;
  endereco: string | null;
  area: number | null;
  dataInicioLote: string;
  dataInicioConstrucao: string | null;
  dataConclusaoObra: string | null;
  custoEstimadoObra: number | null;
  previsaoConclusao: string | null;
  compraValor: number | null;
  compraData: string | null;
  compraVendedorId: number | null;
  compraVendedorNome: string | null;
  vendaValor: number | null;
  vendaData: string | null;
  vendaCompradorId: number | null;
  vendaCompradorNome: string | null;
  vendaValorPretendido: number | null;
  descricao: string | null;
  ativo: boolean;
  fotoPrincipalUrl: string | null;
  aviso: string | null;
}

export interface ImovelFaseRequestDTO {
  novaFase: FaseImovel;
  data: string;
}

export interface ImovelSituacaoRequestDTO {
  novaSituacao: SituacaoImovel;
  valorVenda: number | null;
  dataVenda: string | null;
  compradorId: number | null;
}

export interface ImovelFotoResponseDTO {
  id: number;
  imovelId: number;
  url: string;
  legenda: string | null;
  dataUpload: string;
  principal: boolean;
}
