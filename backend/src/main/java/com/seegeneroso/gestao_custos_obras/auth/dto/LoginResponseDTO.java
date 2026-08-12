package com.seegeneroso.gestao_custos_obras.auth.dto;

public record LoginResponseDTO(
        String token,
        String nome,
        String email,
        String role
) {}
