package com.seegeneroso.gestao_custos_obras.aportante.dto;

public record AportanteResponseDTO(
        Long id,
        String nome,
        String documento,
        String email,
        String telefone,
        String tipoParticipacao,
        Boolean ativo
) {}
