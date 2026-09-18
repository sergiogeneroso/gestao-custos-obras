package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoEstornoRequestDTO(
        @NotNull(message = "Data do estorno é obrigatória")
        LocalDate data,

        @NotNull(message = "Valor do estorno é obrigatório")
        @Positive(message = "Valor do estorno deve ser maior que zero")
        BigDecimal valor
) {}
