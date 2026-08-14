package com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrcamentoCategoriaRequestDTO(
        @NotNull(message = "O imóvel é obrigatório")
        Long imovelId,

        @NotNull(message = "A categoria de despesa é obrigatória")
        Long categoriaDespesaId,

        @NotNull(message = "O valor orçado é obrigatório")
        @Positive(message = "O valor orçado deve ser positivo")
        BigDecimal valorOrcado,

        LocalDate dataInicioPrevista,

        LocalDate dataFimPrevista
) {
}
