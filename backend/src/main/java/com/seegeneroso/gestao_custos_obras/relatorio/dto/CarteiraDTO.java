package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;

import java.math.BigDecimal;
import java.util.Map;

public record CarteiraDTO(
        BigDecimal totalInvestido,
        BigDecimal totalVendido,
        BigDecimal lucroRealizado,
        Map<FaseImovel, Long> imoveisPorFase,
        Map<SituacaoImovel, Long> imoveisPorSituacao,
        BigDecimal saldoDevedorTotal,
        Long parcelasAVencer30Dias,
        BigDecimal gastosGeraisPeriodo
) {
}
