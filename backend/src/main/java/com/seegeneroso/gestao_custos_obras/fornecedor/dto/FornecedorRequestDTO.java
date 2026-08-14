package com.seegeneroso.gestao_custos_obras.fornecedor.dto;

import jakarta.validation.constraints.NotNull;

public record FornecedorRequestDTO(
        @NotNull(message = "ID da pessoa é obrigatório")
        Long pessoaId,

        String areaAtuacao,
        String observacoes
) {}
