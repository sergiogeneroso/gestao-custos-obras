package com.seegeneroso.gestao_custos_obras.relatorio.dto;

import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto.OrcamentoCategoriaResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public record OrcadoVsRealizadoDTO(
        Long imovelId,
        String identificador,
        BigDecimal valorOrcadoTotal,
        BigDecimal valorRealizadoTotal,
        BigDecimal diferenca,
        List<OrcamentoCategoriaResponseDTO> categorias
) {
}
