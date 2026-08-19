import { SituacaoContrato, TipoContratoFinanceiro } from '../contratos/contrato.model';
import { EtapaConstrucao } from '../despesas/despesa.model';
import { FaseImovel, SituacaoImovel } from '../imoveis/imovel.model';

// Posição de caixa do contrato: totalPago e saldo nunca entram no custo do imóvel (ADR-025).
export interface PosicaoContratoDTO {
  contratoId: number;
  tipo: TipoContratoFinanceiro;
  contraparteNome: string | null;
  situacao: SituacaoContrato;
  valorContratado: number;
  totalPago: number;
  saldoDevedor: number;
}

// margem e rentabilidadeAnualizada chegam como fração (0.3333 = 33,33%).
export interface ResultadoImovelDTO {
  imovelId: number;
  identificador: string;
  fase: FaseImovel;
  situacao: SituacaoImovel;
  valorCompra: number | null;
  despesasPorFase: Partial<Record<FaseImovel, number>>;
  totalDespesas: number;
  jurosPagos: number;
  custoTotal: number;
  custoEstimadoObra: number | null;
  previsaoConclusao: string | null;
  custoRealObra: number;
  ajusteQuitacao: number;
  totalDesembolsado: number;
  saldoAPagar: number;
  despesasPorEtapa: Partial<Record<EtapaConstrucao, number>>;
  valorVenda: number | null;
  valorVendaPretendido: number | null;
  dataVenda: string | null;
  lucro: number | null;
  margem: number | null;
  diasEmCarteira: number;
  tempoPorFase: Partial<Record<FaseImovel, number>>;
  rentabilidadeAnualizada: number | null;
  resultadoProvisorio: boolean;
  contratos: PosicaoContratoDTO[];
}
