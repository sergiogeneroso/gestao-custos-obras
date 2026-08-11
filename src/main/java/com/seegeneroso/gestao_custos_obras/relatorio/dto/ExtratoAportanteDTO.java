package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import java.math.BigDecimal;

public record ExtratoAportanteDTO(
        Long aportanteId,
        String nome,
        BigDecimal totalAportado
) {
}
