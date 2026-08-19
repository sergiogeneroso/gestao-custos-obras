package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoDocumentoContrato;

import java.time.LocalDateTime;

public record ContratoDocumentoResponseDTO(
        Long id,
        Long contratoId,
        TipoDocumentoContrato tipoDocumento,
        String url,
        String nomeArquivo,
        String descricao,
        LocalDateTime dataUpload
) {}
