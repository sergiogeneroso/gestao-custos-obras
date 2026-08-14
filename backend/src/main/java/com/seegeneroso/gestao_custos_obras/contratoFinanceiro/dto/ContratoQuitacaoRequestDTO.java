package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoQuitacaoRequestDTO(
        @NotNull(message = "Data de quitação é obrigatória")
        LocalDate dataQuitacao,

        @NotNull(message = "Valor de quitação é obrigatório")
        @Positive(message = "Valor de quitação deve ser maior que zero")
        BigDecimal valorQuitacao
) {}
