package com.seegeneroso.gestao_custos_obras.imovel.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// A transição leva os dados da fase de destino (ADR-033): construcao ao virar CONSTRUCAO, casa ao
// virar CASA. Ambos opcionais — alvará e habite-se costumam sair depois do fato.
public record ImovelFaseRequestDTO(
        @NotNull(message = "Nova fase é obrigatória")
        FaseImovel novaFase,

        @NotNull(message = "Data da transição é obrigatória")
        LocalDate data,

        DadosConstrucaoDTO construcao,
        DadosCasaDTO casa
) {}
