package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelaContratoResponseDTO(
        Long id,
        Integer numero,
        LocalDate dataVencimento,
        BigDecimal valor,
        BigDecimal valorJuros,
        LocalDate dataPagamento,
        BigDecimal valorPago
) {}
