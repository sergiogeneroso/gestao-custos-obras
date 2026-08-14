package com.seegeneroso.gestao_custos_obras.imovel.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoDocumentoImovel;

import java.time.LocalDateTime;

public record ImovelDocumentoResponseDTO(
        Long id,
        Long imovelId,
        TipoDocumentoImovel tipoDocumento,
        FaseImovel faseImovel,
        String url,
        LocalDateTime dataUpload
) {}
