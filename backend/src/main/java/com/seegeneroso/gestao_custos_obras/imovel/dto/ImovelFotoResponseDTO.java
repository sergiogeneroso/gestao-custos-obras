package com.seegeneroso.gestao_custos_obras.imovel.dto;

import java.time.LocalDateTime;

public record ImovelFotoResponseDTO(
        Long id,
        Long imovelId,
        String url,
        String legenda,
        LocalDateTime dataUpload,
        Boolean principal
) {
}
