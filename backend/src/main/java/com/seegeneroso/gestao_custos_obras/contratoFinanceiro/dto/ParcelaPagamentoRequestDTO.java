package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelaPagamentoRequestDTO(
        @NotNull(message = "Data de pagamento é obrigatória")
        LocalDate dataPagamento,

        @NotNull(message = "Valor pago é obrigatório")
        @Positive(message = "Valor pago deve ser maior que zero")
        BigDecimal valorPago
) {}
