package com.seegeneroso.gestao_custos_obras.despesa.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoAnexoDespesa;

import java.time.LocalDateTime;

public record DespesaAnexoResponseDTO(
        Long id,
        Long despesaId,
        TipoAnexoDespesa tipoAnexo,
        String url,
        LocalDateTime dataUpload
) {}
