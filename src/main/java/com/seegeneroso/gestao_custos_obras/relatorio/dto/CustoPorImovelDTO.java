package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import java.math.BigDecimal;

public record CustoPorImovelDTO(
        Long imovelId,
        String identificador,
        BigDecimal custoTotal
) {
}
