package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import java.math.BigDecimal;

public record CustoPorM2DTO(
        Long imovelId,
        String identificador,
        BigDecimal areaLote,
        BigDecimal areaConstruida,
        BigDecimal custoTotal,
        BigDecimal custoPorM2,
        BigDecimal custoObra,
        BigDecimal custoObraPorM2
) {
}
