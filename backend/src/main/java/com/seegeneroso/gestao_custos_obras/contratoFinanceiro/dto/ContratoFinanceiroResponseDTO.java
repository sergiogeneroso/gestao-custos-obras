package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContratoFinanceiroResponseDTO(
        Long id,
        Long imovelId,
        String imovelIdentificador,
        TipoContratoFinanceiro tipo,
        Long contraparteId,
        String contraparteNome,
        BigDecimal valorContratado,
        SituacaoContrato situacao,
        LocalDate dataQuitacao,
        BigDecimal valorQuitacao,
        List<ParcelaContratoResponseDTO> parcelas
) {}
