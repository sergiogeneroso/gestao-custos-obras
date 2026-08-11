package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import java.math.BigDecimal;

public record CustoPorM2DTO(
        Long imovelId,
        String identificador,
        BigDecimal area,
        BigDecimal custoTotal,
        BigDecimal custoPorM2
) {
}
