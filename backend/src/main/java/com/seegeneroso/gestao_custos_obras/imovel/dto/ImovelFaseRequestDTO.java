package com.seegeneroso.gestao_custos_obras.imovel.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ImovelFaseRequestDTO(
        @NotNull(message = "Nova fase é obrigatória")
        FaseImovel novaFase,

        @NotNull(message = "Data da transição é obrigatória")
        LocalDate data
) {}
