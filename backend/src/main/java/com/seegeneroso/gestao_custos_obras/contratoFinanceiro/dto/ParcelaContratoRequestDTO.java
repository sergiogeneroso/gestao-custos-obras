package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelaContratoRequestDTO(
        // Zero é a entrada da compra parcelada (ADR-037); as prestações começam em 1.
        @NotNull(message = "Número da parcela é obrigatório")
        @PositiveOrZero(message = "Número da parcela não pode ser negativo")
        Integer numero,

        @NotNull(message = "Data de vencimento é obrigatória")
        LocalDate dataVencimento,

        @NotNull(message = "Valor da parcela é obrigatório")
        @Positive(message = "Valor da parcela deve ser maior que zero")
        BigDecimal valor,

        BigDecimal valorJuros
) {}
