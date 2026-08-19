package com.seegeneroso.gestao_custos_obras.despesa.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.EtapaConstrucao;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaRequestDTO(
        Long imovelId,

        @NotNull(message = "Categoria de despesa é obrigatória")
        Long categoriaDespesaId,

        @NotNull(message = "Pagador é obrigatório")
        Long pagadorId,

        Long beneficiarioId,
        Long contratoFinanceiroId,
        FaseImovel faseImovel,
        EtapaConstrucao etapaConstrucao,

        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser maior que zero")
        BigDecimal valor,

        @NotNull(message = "Data de pagamento é obrigatória")
        LocalDate dataPagamento,

        String descricao
) {}
