package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import java.math.BigDecimal;

public record AlocacaoLoteResponseDTO(
        Long imovelId,
        String imovelIdentificador,
        BigDecimal valorAlocado
) {
}
