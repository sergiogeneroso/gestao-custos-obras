package com.seegeneroso.gestao_custos_obras.imovel.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImovelSituacaoRequestDTO(
        @NotNull(message = "Nova situação é obrigatória")
        SituacaoImovel novaSituacao,

        BigDecimal valorVenda,
        LocalDate dataVenda,
        Long compradorId,

        // Só faz sentido ao colocar à venda; é o momento em que o valor pretendido é decidido.
        BigDecimal vendaValorPretendido,

        // Obrigatório só ao desfazer uma venda (sair de VENDIDO) — validado em ImovelService,
        // porque a situação atual não está disponível aqui no DTO (ADR-043).
        String motivo
) {

    @AssertTrue(message = "Situação VENDIDO exige valor, data e comprador da venda")
    public boolean isVendaValida() {
        if (novaSituacao != SituacaoImovel.VENDIDO) {
            return true;
        }
        return valorVenda != null && dataVenda != null && compradorId != null;
    }
}
