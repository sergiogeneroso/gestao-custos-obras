package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import java.math.BigDecimal;

public record ExtratoPessoaDTO(
        Long pessoaId,
        String nome,
        BigDecimal totalPago
) {
}
