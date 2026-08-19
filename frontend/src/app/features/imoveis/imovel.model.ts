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

// Um grupo por fase do ciclo (ADR-031); o cadastro só preenche o do lote (ADR-033).
export interface DadosLoteDTO {
  matricula: string | null;
  cartorio: string | null;
  dataRegistro: string | null;
  inscricaoMunicipal: string | null;
  area: number | null;
}

export interface DadosConstrucaoDTO {
  area: number | null;
  dataInicio: string | null;
  previsaoConclusao: string | null;
  custoEstimado: number | null;
  alvaraNumero: string | null;
  alvaraEmissao: string | null;
  alvaraValidade: string | null;
  artNumero: string | null;
  responsavelTecnicoId: number | null;
  responsavelTecnicoNome: string | null;
  cno: string | null;
}

export interface DadosCasaDTO {
  dataConclusaoObra: string | null;
  habiteSeNumero: string | null;
  habiteSeData: string | null;
  dataAverbacao: string | null;
  quartos: number | null;
  suites: number | null;
  banheiros: number | null;
  vagasGaragem: number | null;
}

export interface ImovelRequestDTO {
  identificador: string;
  endereco: string | null;
  numero: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
  cep: string | null;
  observacaoEndereco: string | null;
  lote: DadosLoteDTO | null;
  construcao: DadosConstrucaoDTO | null;
  casa: DadosCasaDTO | null;
  compraValor: number | null;
  compraData: string;
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
  numero: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
  cep: string | null;
  observacaoEndereco: string | null;
  lote: DadosLoteDTO;
  construcao: DadosConstrucaoDTO;
  casa: DadosCasaDTO;
  compraValor: number | null;
  compraData: string;
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
  // Dados da fase de destino; o dialog que os coleta é a Etapa M.
  construcao?: Partial<DadosConstrucaoDTO> | null;
  casa?: Partial<DadosCasaDTO> | null;
}

export interface ImovelSituacaoRequestDTO {
  novaSituacao: SituacaoImovel;
  valorVenda: number | null;
  dataVenda: string | null;
  compradorId: number | null;
  vendaValorPretendido?: number | null;
}

export interface ImovelFotoResponseDTO {
  id: number;
  imovelId: number;
  url: string;
  legenda: string | null;
  dataUpload: string;
  principal: boolean;
}

export type TipoDocumentoImovel =
  | 'MATRICULA'
  | 'ESCRITURA'
  | 'CONTRATO'
  | 'IPTU'
  | 'ALVARA'
  | 'PROJETO'
  | 'ART'
  | 'HABITE_SE'
  | 'OUTRO';

export const TIPO_DOCUMENTO_IMOVEL_LABEL: Record<TipoDocumentoImovel, string> = {
  MATRICULA: 'Matrícula',
  ESCRITURA: 'Escritura',
  CONTRATO: 'Contrato',
  IPTU: 'IPTU',
  ALVARA: 'Alvará',
  PROJETO: 'Projeto',
  ART: 'ART / RRT',
  HABITE_SE: 'Habite-se',
  OUTRO: 'Outro',
};

export interface ImovelDocumentoResponseDTO {
  id: number;
  imovelId: number;
  tipoDocumento: TipoDocumentoImovel;
  faseImovel: FaseImovel;
  url: string;
  nomeArquivo: string | null;
  descricao: string | null;
  dataEmissao: string | null;
  dataValidade: string | null;
  dataUpload: string;
}

// Metadados que acompanham o upload; o arquivo vai no FormData.
export interface EnvioDocumento {
  tipoDocumento: TipoDocumentoImovel;
  faseImovel: FaseImovel | null;
  descricao: string | null;
  dataEmissao: string | null;
  dataValidade: string | null;
}
