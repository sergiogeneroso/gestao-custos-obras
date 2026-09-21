package com.seegeneroso.gestao_custos_obras.perfil.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;

import java.util.Map;
import java.util.Set;

// Consultado pelo frontend logo após o login pra decidir o que mostrar (ADR-046) — não embutido
// como claim no JWT, porque a matriz é editável em runtime e um claim ficaria defasado.
public record MinhasPermissoesResponseDTO(
        String perfilNome,
        Map<DominioSistema, Set<AcaoPermissao>> permissoes
) {
}
