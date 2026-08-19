package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.EtapaConstrucao;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ResultadoImovelDTO(
        Long imovelId,
        String identificador,
        FaseImovel fase,
        SituacaoImovel situacao,
        BigDecimal valorCompra,
        Map<FaseImovel, BigDecimal> despesasPorFase,
        BigDecimal totalDespesas,
        BigDecimal jurosPagos,
        BigDecimal custoTotal,
        BigDecimal custoEstimadoObra,
        LocalDate previsaoConclusao,
        BigDecimal custoRealObra,
        Map<EtapaConstrucao, BigDecimal> despesasPorEtapa,
        BigDecimal ajusteQuitacao,
        BigDecimal totalDesembolsado,
        BigDecimal saldoAPagar,
        BigDecimal valorVenda,
        BigDecimal valorVendaPretendido,
        LocalDate dataVenda,
        BigDecimal lucro,
        BigDecimal margem,
        Long diasEmCarteira,
        Map<FaseImovel, Long> tempoPorFase,
        Double rentabilidadeAnualizada,
        Boolean resultadoProvisorio,
        List<PosicaoContratoDTO> contratos
) {
}
