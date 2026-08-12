package com.seegeneroso.gestao_custos_obras.imovel.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.StatusImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoImovel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ImovelRequestDTO(

        @NotBlank(message = "Identificador é obrigatório")
        String identificador,

        @NotNull(message = "Tipo é obrigatório")
        TipoImovel tipo,

        String endereco,
        BigDecimal area,
        BigDecimal valorAquisicaoInicial,
        StatusImovel status,
        String descricao
) {}