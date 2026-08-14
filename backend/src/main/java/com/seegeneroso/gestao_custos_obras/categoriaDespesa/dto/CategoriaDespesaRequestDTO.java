package com.seegeneroso.gestao_custos_obras.categoriaDespesa.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaDespesaRequestDTO(
        @NotBlank(message = "Nome da categoria é obrigatório")
        String nome,

        String descricao
) {}
