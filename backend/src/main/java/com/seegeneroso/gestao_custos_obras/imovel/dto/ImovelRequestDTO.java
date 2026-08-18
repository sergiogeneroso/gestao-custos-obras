package com.seegeneroso.gestao_custos_obras.imovel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImovelRequestDTO(

        @NotBlank(message = "Identificador é obrigatório")
        String identificador,

        String endereco,
        BigDecimal areaLote,
        BigDecimal areaConstruida,

        @NotNull(message = "Data de início como lote é obrigatória")
        LocalDate dataInicioLote,

        LocalDate dataInicioConstrucao,
        LocalDate dataConclusaoObra,
        BigDecimal custoEstimadoObra,
        LocalDate previsaoConclusao,

        BigDecimal compraValor,
        LocalDate compraData,
        Long compraVendedorId,

        BigDecimal vendaValorPretendido,

        String descricao
) {}
