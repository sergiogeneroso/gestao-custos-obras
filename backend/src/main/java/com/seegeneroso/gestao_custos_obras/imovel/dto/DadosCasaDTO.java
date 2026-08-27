package com.seegeneroso.gestao_custos_obras.imovel.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DadosCasaDTO(
        LocalDate dataConclusaoObra,

        @Size(max = 50, message = "Número do habite-se deve ter no máximo 50 caracteres")
        String habiteSeNumero,

        LocalDate habiteSeData,
        LocalDate dataAverbacao,
        Integer quartos,
        Integer suites,
        Integer banheiros,
        Integer vagasGaragem
) {}
