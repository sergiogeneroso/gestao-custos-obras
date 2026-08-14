package com.seegeneroso.gestao_custos_obras.imovel.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImovelResponseDTO(
        Long id,
        String identificador,
        FaseImovel fase,
        SituacaoImovel situacao,
        String endereco,
        BigDecimal area,
        LocalDate dataInicioLote,
        LocalDate dataInicioConstrucao,
        LocalDate dataConclusaoObra,
        BigDecimal custoEstimadoObra,
        LocalDate previsaoConclusao,
        BigDecimal compraValor,
        LocalDate compraData,
        Long compraVendedorId,
        String compraVendedorNome,
        BigDecimal vendaValor,
        LocalDate vendaData,
        Long vendaCompradorId,
        String vendaCompradorNome,
        BigDecimal vendaValorPretendido,
        String descricao,
        Boolean ativo,
        String fotoPrincipalUrl,
        String aviso
) {}
