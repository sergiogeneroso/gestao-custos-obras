package com.seegeneroso.gestao_custos_obras.imovel.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImovelSituacaoRequestDTO(
        @NotNull(message = "Nova situação é obrigatória")
        SituacaoImovel novaSituacao,

        BigDecimal valorVenda,
        LocalDate dataVenda,
        Long compradorId
) {}
