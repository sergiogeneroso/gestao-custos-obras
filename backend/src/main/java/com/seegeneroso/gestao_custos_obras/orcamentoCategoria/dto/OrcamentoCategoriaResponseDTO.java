package com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrcamentoCategoriaResponseDTO(
        Long id,
        Long imovelId,
        String imovelIdentificador,
        Long categoriaDespesaId,
        String categoriaDespesaNome,
        BigDecimal valorOrcado,
        LocalDate dataInicioPrevista,
        LocalDate dataFimPrevista,
        BigDecimal totalGasto,
        BigDecimal diferenca,
        String statusOrcamento
) {
}
