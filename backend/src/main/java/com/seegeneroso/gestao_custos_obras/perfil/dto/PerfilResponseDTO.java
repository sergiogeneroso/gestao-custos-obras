package com.seegeneroso.gestao_custos_obras.perfil.dto;

import java.util.List;

public record PerfilResponseDTO(
        Long id,
        String nome,
        List<PerfilPermissaoDTO> permissoes,
        Boolean ativo
) {
}
