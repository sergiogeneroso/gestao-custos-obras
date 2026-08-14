package com.seegeneroso.gestao_custos_obras.fornecedor.dto;

public record FornecedorResponseDTO(
        Long id,
        Long pessoaId,
        String pessoaNome,
        String areaAtuacao,
        String observacoes,
        Boolean ativo
) {}
