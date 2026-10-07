package com.seegeneroso.gestao_custos_obras.despesa.dto;

import jakarta.validation.constraints.NotNull;

/** true marca a despesa como conferida; false desfaz a conferência (ADR-048). */
public record ConferenciaRequestDTO(
        @NotNull(message = "Informe se a despesa está conferida")
        Boolean conferida
) {
}
