package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;

import java.math.BigDecimal;

// Posição de caixa do contrato — totalPago e saldoDevedor nunca entram no custoTotal do imóvel (ADR-025).
public record PosicaoContratoDTO(
        Long contratoId,
        TipoContratoFinanceiro tipo,
        String contraparteNome,
        SituacaoContrato situacao,
        BigDecimal valorContratado,
        BigDecimal totalPago,
        BigDecimal saldoDevedor
) {
}
