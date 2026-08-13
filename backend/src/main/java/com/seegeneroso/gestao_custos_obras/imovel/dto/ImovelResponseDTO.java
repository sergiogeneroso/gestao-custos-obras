package com.seegeneroso.gestao_custos_obras.imovel.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.StatusImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoImovel;

import java.math.BigDecimal;

public record ImovelResponseDTO(
        Long id,
        String identificador,
        TipoImovel tipo,
        String endereco,
        BigDecimal area,
        BigDecimal valorAquisicaoInicial,
        StatusImovel status,
        String descricao,
        Boolean ativo,
        String fotoPrincipalUrl
) {}