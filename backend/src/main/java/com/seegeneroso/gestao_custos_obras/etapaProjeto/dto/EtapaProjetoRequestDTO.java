package com.seegeneroso.gestao_custos_obras.etapaProjeto.dto;

import jakarta.validation.constraints.NotBlank;

public record EtapaProjetoRequestDTO(
        @NotBlank(message = "Nome da etapa é obrigatório")
        String nome,

        String descricao
) {}
