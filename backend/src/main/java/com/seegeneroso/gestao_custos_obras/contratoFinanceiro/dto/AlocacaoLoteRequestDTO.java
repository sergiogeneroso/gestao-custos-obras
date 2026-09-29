package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// Fatia de um lote dentro de um contrato compartilhado (ADR-047). valorAlocado é opcional só
// quando o contrato cobre um único lote — nesse caso vale o contrato inteiro (mesmo
// comportamento de antes da ADR-047); com mais de um lote é obrigatório, porque não há como
// inferir a divisão sem o usuário declarar.
public record AlocacaoLoteRequestDTO(
        @NotNull(message = "Imóvel é obrigatório")
        Long imovelId,

        @Positive(message = "Valor alocado deve ser maior que zero")
        BigDecimal valorAlocado
) {
}
