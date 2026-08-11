package com.seegeneroso.gestao_custos_obras.aportante.dto;

import jakarta.validation.constraints.NotBlank;

public record AportanteRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        String documento,
        String email,
        String telefone,

        @NotBlank(message = "Tipo de participação é obrigatório")
        String tipoParticipacao
) {}
