package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;

import java.math.BigDecimal;

// Posição de caixa do contrato — totalPago e saldoDevedor nunca entram no custoTotal do imóvel (ADR-025).
// valorAlocado, totalPago e saldoDevedor são a fatia deste lote, não o contrato inteiro (ADR-047);
// compartilhado/quantidadeLotes avisam quando o contrato cobre mais de um lote.
public record PosicaoContratoDTO(
        Long contratoId,
        TipoContratoFinanceiro tipo,
        String contraparteNome,
        SituacaoContrato situacao,
        BigDecimal valorAlocado,
        BigDecimal totalPago,
        BigDecimal saldoDevedor,
        boolean compartilhado,
        int quantidadeLotes
) {
}
