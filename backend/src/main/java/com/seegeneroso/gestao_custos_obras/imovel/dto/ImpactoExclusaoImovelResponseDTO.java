package com.seegeneroso.gestao_custos_obras.imovel.dto;

public record ImpactoExclusaoImovelResponseDTO(
        long despesas,
        long contratosFinanceiros,
        long fotos,
        long documentos,
        long orcamentosCategoria
) {
}
