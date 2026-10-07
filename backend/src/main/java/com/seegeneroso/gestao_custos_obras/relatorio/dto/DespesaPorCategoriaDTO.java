package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import java.math.BigDecimal;

// Recorte de apresentação, como despesasPorEtapa: nunca somar ao custoTotal do imóvel.
public record DespesaPorCategoriaDTO(
        Long categoriaDespesaId,
        String categoriaDespesaNome,
        BigDecimal total
) {
}
