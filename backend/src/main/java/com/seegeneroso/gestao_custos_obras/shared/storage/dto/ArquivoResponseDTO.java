package com.seegeneroso.gestao_custos_obras.shared.storage.dto;

public record ArquivoResponseDTO(
        String nomeArquivo,
        String url,
        String contentType,
        long tamanho
) {
}
