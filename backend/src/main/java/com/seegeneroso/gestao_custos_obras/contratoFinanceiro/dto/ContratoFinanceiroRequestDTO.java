package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record ContratoFinanceiroRequestDTO(
        @NotNull(message = "Imóvel é obrigatório")
        Long imovelId,

        @NotNull(message = "Tipo do contrato é obrigatório")
        TipoContratoFinanceiro tipo,

        @NotNull(message = "Contraparte é obrigatória")
        Long contraparteId,

        @NotNull(message = "Valor contratado é obrigatório")
        @Positive(message = "Valor contratado deve ser maior que zero")
        BigDecimal valorContratado,

        List<@Valid ParcelaContratoRequestDTO> parcelas
) {}
