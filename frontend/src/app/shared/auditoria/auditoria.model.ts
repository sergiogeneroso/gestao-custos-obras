export type OperacaoAuditoria = 'CRIACAO' | 'EDICAO' | 'EXCLUSAO';

export const OPERACAO_AUDITORIA_LABEL: Record<OperacaoAuditoria, string> = {
  CRIACAO: 'Criação',
  EDICAO: 'Edição',
  EXCLUSAO: 'Exclusão',
};

export interface LogAuditoriaResponseDTO {
  id: number;
  entidade: string;
  entidadeId: number;
  operacao: OperacaoAuditoria;
  usuarioId: number | null;
  usuarioNome: string | null;
  dataHora: string;
  /** JSON cru do ResponseDTO do domínio — nulo em CRIACAO (antes) ou exclusão física (depois). */
  estadoAnterior: string | null;
  estadoNovo: string | null;
}
