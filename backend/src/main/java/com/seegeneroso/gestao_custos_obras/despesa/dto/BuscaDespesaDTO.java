package com.seegeneroso.gestao_custos_obras.despesa.dto;

import com.seegeneroso.gestao_custos_obras.shared.PaginaDTO;

import java.math.BigDecimal;

// valorTotal soma TODAS as despesas que casam com o filtro, não só as da página.
public record BuscaDespesaDTO(
        PaginaDTO<DespesaResponseDTO> pagina,
        BigDecimal valorTotal
) {
}
