package com.seegeneroso.gestao_custos_obras.despesa.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DespesaRequestDTO(
        @NotNull(message = "ID do imóvel é obrigatório")
        Long imovelId,

        @NotNull(message = "ID da etapa do projeto é obrigatório")
        Long etapaProjetoId,

        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "Data de pagamento é obrigatória")
        LocalDate dataPagamento,

        String descricao,
        String comprovanteUrl,

        @Valid
        List<DespesaPagamentoRequestDTO> pagamentos
) {}
