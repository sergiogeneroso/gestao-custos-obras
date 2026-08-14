package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelaContratoRequestDTO(
        @NotNull(message = "Número da parcela é obrigatório")
        @Positive(message = "Número da parcela deve ser maior que zero")
        Integer numero,

        @NotNull(message = "Data de vencimento é obrigatória")
        LocalDate dataVencimento,

        @NotNull(message = "Valor da parcela é obrigatório")
        @Positive(message = "Valor da parcela deve ser maior que zero")
        BigDecimal valor,

        BigDecimal valorJuros
) {}
