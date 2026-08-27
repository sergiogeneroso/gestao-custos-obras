package com.seegeneroso.gestao_custos_obras.categoriaDespesa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaDespesaRequestDTO(
        @NotBlank(message = "Nome da categoria é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,

        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
        String descricao
) {}
