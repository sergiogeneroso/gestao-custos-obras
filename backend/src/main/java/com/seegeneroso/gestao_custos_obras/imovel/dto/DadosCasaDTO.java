package com.seegeneroso.gestao_custos_obras.imovel.dto;

import java.time.LocalDate;

public record DadosCasaDTO(
        LocalDate dataConclusaoObra,
        String habiteSeNumero,
        LocalDate habiteSeData,
        LocalDate dataAverbacao,
        Integer quartos,
        Integer suites,
        Integer banheiros,
        Integer vagasGaragem
) {}
