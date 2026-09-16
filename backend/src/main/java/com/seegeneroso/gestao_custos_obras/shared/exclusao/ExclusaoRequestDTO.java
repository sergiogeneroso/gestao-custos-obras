package com.seegeneroso.gestao_custos_obras.shared.exclusao;

import jakarta.validation.constraints.NotBlank;

public record ExclusaoRequestDTO(
        @NotBlank(message = "Motivo da exclusão é obrigatório")
        String motivo
) {
}
