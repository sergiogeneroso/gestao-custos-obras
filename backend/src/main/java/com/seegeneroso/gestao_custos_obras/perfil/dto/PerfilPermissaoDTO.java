package com.seegeneroso.gestao_custos_obras.perfil.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;
import jakarta.validation.constraints.NotNull;

public record PerfilPermissaoDTO(
        @NotNull(message = "Domínio é obrigatório")
        DominioSistema dominio,

        @NotNull(message = "Ação é obrigatória")
        AcaoPermissao acao
) {
}
