// Espelha shared/enums/DominioSistema.java e AcaoPermissao.java (ADR-046) — lista fixa,
// só a matriz perfil×domínio×ação é editável em runtime.
export type DominioSistema =
  | 'IMOVEL'
  | 'PESSOA'
  | 'DESPESA'
  | 'CONTRATO_FINANCEIRO'
  | 'CATEGORIA_DESPESA'
  | 'RELATORIO'
  | 'AUDITORIA'
  | 'PERFIL';

export type AcaoPermissao = 'ACESSAR' | 'INCLUIR' | 'ALTERAR' | 'DELETAR';

export const DOMINIOS: DominioSistema[] = [
  'IMOVEL',
  'PESSOA',
  'DESPESA',
  'CONTRATO_FINANCEIRO',
  'CATEGORIA_DESPESA',
  'RELATORIO',
  'AUDITORIA',
  'PERFIL',
];

export const ACOES: AcaoPermissao[] = ['ACESSAR', 'INCLUIR', 'ALTERAR', 'DELETAR'];

export const DOMINIO_LABEL: Record<DominioSistema, string> = {
  IMOVEL: 'Imóveis',
  PESSOA: 'Pessoas',
  DESPESA: 'Despesas',
  CONTRATO_FINANCEIRO: 'Contratos financeiros',
  CATEGORIA_DESPESA: 'Categorias de despesa',
  RELATORIO: 'Relatórios',
  AUDITORIA: 'Auditoria',
  PERFIL: 'Perfis',
};

export const ACAO_LABEL: Record<AcaoPermissao, string> = {
  ACESSAR: 'Acessar',
  INCLUIR: 'Incluir',
  ALTERAR: 'Alterar',
  DELETAR: 'Deletar',
};

// Relatório e Auditoria só têm endpoint de leitura — as outras três colunas nunca têm efeito
// (ADR-046), então a tela de administração de Perfil as esconde.
export const DOMINIOS_SO_LEITURA: DominioSistema[] = ['RELATORIO', 'AUDITORIA'];

export interface PerfilPermissaoDTO {
  dominio: DominioSistema;
  acao: AcaoPermissao;
}

export interface PerfilRequestDTO {
  nome: string;
  permissoes: PerfilPermissaoDTO[];
}

export interface PerfilResponseDTO {
  id: number;
  nome: string;
  permissoes: PerfilPermissaoDTO[];
  ativo: boolean;
}

export interface MinhasPermissoesResponseDTO {
  perfilNome: string | null;
  permissoes: Partial<Record<DominioSistema, AcaoPermissao[]>>;
}
