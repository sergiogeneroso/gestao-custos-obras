import { ACOES, AcaoPermissao, DOMINIOS, DominioSistema, PerfilPermissaoDTO } from '../../core/auth/permissoes.model';

export type MatrizPermissao = Record<DominioSistema, Record<AcaoPermissao, boolean>>;

export function matrizDePermissoes(permissoes: PerfilPermissaoDTO[]): MatrizPermissao {
  const matriz = Object.fromEntries(
    DOMINIOS.map((dominio) => [dominio, Object.fromEntries(ACOES.map((acao) => [acao, false]))]),
  ) as MatrizPermissao;

  for (const permissao of permissoes) {
    matriz[permissao.dominio][permissao.acao] = true;
  }
  return matriz;
}

/**
 * "Acessar" é pré-requisito das outras três ações no mesmo domínio (ADR-046): desmarcar acessar
 * limpa o resto; marcar qualquer outra ação liga acessar junto — evita montar em tela uma
 * combinação que o backend recusaria ao salvar (`PerfilRequestDTO.isAcessarPrerequisitoDasDemais`).
 */
export function alternarPermissao(matriz: MatrizPermissao, dominio: DominioSistema, acao: AcaoPermissao): MatrizPermissao {
  const linha = { ...matriz[dominio], [acao]: !matriz[dominio][acao] };
  if (acao === 'ACESSAR' && !linha.ACESSAR) {
    linha.INCLUIR = false;
    linha.ALTERAR = false;
    linha.DELETAR = false;
  } else if (acao !== 'ACESSAR' && linha[acao]) {
    linha.ACESSAR = true;
  }
  return { ...matriz, [dominio]: linha };
}

export function permissoesDaMatriz(matriz: MatrizPermissao): PerfilPermissaoDTO[] {
  return DOMINIOS.flatMap((dominio) =>
    ACOES.filter((acao) => matriz[dominio][acao]).map((acao) => ({ dominio, acao })),
  );
}
