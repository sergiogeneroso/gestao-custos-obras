package com.seegeneroso.gestao_custos_obras.orcamentoEtapa.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrcamentoEtapaRequestDTO(
        @NotNull(message = "O imóvel é obrigatório")
        Long imovelId,

        @NotNull(message = "A etapa do projeto é obrigatória")
        Long etapaProjetoId,

        @NotNull(message = "O valor orçado é obrigatório")
        @Positive(message = "O valor orçado deve ser positivo")
        BigDecimal valorOrcado,

        LocalDate dataInicioPrevista,

        LocalDate dataFimPrevista
) {
}
