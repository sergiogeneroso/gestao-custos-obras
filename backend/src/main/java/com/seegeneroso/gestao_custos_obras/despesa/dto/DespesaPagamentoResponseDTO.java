package com.seegeneroso.gestao_custos_obras.despesa.dto;

import java.math.BigDecimal;

public record DespesaPagamentoResponseDTO(
        Long id,
        Long aportanteId,
        String aportanteNome,
        BigDecimal valorPago
) {}
