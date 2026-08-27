package com.seegeneroso.gestao_custos_obras.imovel.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// Os @Size espelham o length das colunas de DadosLote: o limite precisa ser recusado com o nome
// do campo, não como violação de integridade do banco.
public record DadosLoteDTO(
        @Size(max = 50, message = "Matrícula deve ter no máximo 50 caracteres")
        String matricula,

        @Size(max = 150, message = "Cartório deve ter no máximo 150 caracteres")
        String cartorio,

        LocalDate dataRegistro,

        @Size(max = 50, message = "Inscrição municipal deve ter no máximo 50 caracteres")
        String inscricaoMunicipal,

        BigDecimal area
) {}
