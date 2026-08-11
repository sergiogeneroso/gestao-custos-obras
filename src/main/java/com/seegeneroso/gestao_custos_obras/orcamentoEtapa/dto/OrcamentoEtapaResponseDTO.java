package com.seegeneroso.gestao_custos_obras.orcamentoEtapa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrcamentoEtapaResponseDTO(
        Long id,
        Long imovelId,
        String imovelIdentificador,
        Long etapaProjetoId,
        String etapaProjetoNome,
        BigDecimal valorOrcado,
        LocalDate dataInicioPrevista,
        LocalDate dataFimPrevista,
        BigDecimal totalGasto,
        BigDecimal diferenca,
        String statusOrcamento
) {
}
