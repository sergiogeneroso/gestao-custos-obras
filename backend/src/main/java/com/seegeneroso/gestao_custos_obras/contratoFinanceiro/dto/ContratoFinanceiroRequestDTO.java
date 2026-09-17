package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

import java.math.BigDecimal;
import java.util.List;

public record ContratoFinanceiroRequestDTO(
        @NotNull(message = "Imóvel é obrigatório")
        Long imovelId,

        @NotNull(message = "Tipo do contrato é obrigatório")
        TipoContratoFinanceiro tipo,

        @NotNull(message = "Contraparte é obrigatória")
        Long contraparteId,

        @NotNull(message = "Valor contratado é obrigatório")
        @Positive(message = "Valor contratado deve ser maior que zero")
        BigDecimal valorContratado,

        List<@Valid ParcelaContratoRequestDTO> parcelas,

        // Entrada da compra parcelada: vira a parcela nº 0, já baixada na data informada (ADR-037).
        @PositiveOrZero(message = "Valor da entrada não pode ser negativo")
        BigDecimal entradaValor,
        LocalDate entradaData,

        // Entrada de cálculo, não gravada no contrato: quando informado, é o preço à vista do lote e
        // a diferença para o cronograma são juros. Em branco, o preço do lote é o próprio total.
        @Positive(message = "Preço à vista do lote deve ser maior que zero")
        BigDecimal precoAVistaLote
) {

    // A entrada vira a parcela nº 0 à parte de `parcelas` (ver ContratoFinanceiroService.montarEntrada),
    // então um contrato quitado inteiro na entrada — sem nenhuma parcela futura — é válido.
    @AssertTrue(message = "O contrato precisa de ao menos uma parcela ou uma entrada")
    public boolean isCronogramaValido() {
        boolean temParcelas = parcelas != null && !parcelas.isEmpty();
        boolean temEntrada = entradaValor != null && entradaValor.compareTo(BigDecimal.ZERO) > 0;
        return temParcelas || temEntrada;
    }
}
