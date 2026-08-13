export type TipoImovel = 'LOTE' | 'IMOVEL';
export type StatusImovel = 'PLANEJAMENTO' | 'CONSTRUCAO' | 'FINALIZADO';

export const TIPO_IMOVEL_LABEL: Record<TipoImovel, string> = {
  LOTE: 'Lote',
  IMOVEL: 'Imóvel',
};

export const STATUS_IMOVEL_LABEL: Record<StatusImovel, string> = {
  PLANEJAMENTO: 'Planejamento',
  CONSTRUCAO: 'Construção',
  FINALIZADO: 'Finalizado',
};

export interface ImovelRequestDTO {
  identificador: string;
  tipo: TipoImovel;
  endereco: string | null;
  area: number | null;
  valorAquisicaoInicial: number | null;
  status: StatusImovel;
  descricao: string | null;
}

export interface ImovelResponseDTO {
  id: number;
  identificador: string;
  tipo: TipoImovel;
  endereco: string | null;
  area: number | null;
  valorAquisicaoInicial: number | null;
  status: StatusImovel;
  descricao: string | null;
  ativo: boolean;
  fotoPrincipalUrl: string | null;
}

export interface ImovelFotoResponseDTO {
  id: number;
  imovelId: number;
  url: string;
  legenda: string | null;
  dataUpload: string;
  principal: boolean;
}
