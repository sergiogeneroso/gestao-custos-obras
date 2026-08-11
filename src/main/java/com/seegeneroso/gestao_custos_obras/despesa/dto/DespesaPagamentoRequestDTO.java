package com.seegeneroso.gestao_custos_obras.despesa.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DespesaPagamentoRequestDTO(
        @NotNull(message = "ID do aportante é obrigatório")
        Long aportanteId,

        @NotNull(message = "Valor pago é obrigatório")
        @Positive(message = "Valor pago deve ser maior que zero")
        BigDecimal valorPago
) {}
