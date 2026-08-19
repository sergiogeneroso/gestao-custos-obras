package com.seegeneroso.gestao_custos_obras.imovel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosLoteDTO(
        String matricula,
        String cartorio,
        LocalDate dataRegistro,
        String inscricaoMunicipal,
        BigDecimal area
) {}
