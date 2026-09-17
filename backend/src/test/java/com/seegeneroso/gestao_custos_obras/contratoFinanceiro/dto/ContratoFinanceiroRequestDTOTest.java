package com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContratoFinanceiroRequestDTOTest {

    private ContratoFinanceiroRequestDTO dto(List<ParcelaContratoRequestDTO> parcelas, BigDecimal entradaValor) {
        return new ContratoFinanceiroRequestDTO(
                1L, TipoContratoFinanceiro.PARCELAMENTO_COMPRA, 2L, new BigDecimal("100000"),
                parcelas, entradaValor, entradaValor != null ? LocalDate.now() : null, null
        );
    }

    @Test
    void recusaContratoSemParcelaENemEntrada() {
        assertThat(dto(List.of(), null).isCronogramaValido()).isFalse();
        assertThat(dto(null, null).isCronogramaValido()).isFalse();
        assertThat(dto(List.of(), BigDecimal.ZERO).isCronogramaValido()).isFalse();
    }

    @Test
    void aceitaContratoComAoMenosUmaParcela() {
        var parcela = new ParcelaContratoRequestDTO(1, LocalDate.now(), new BigDecimal("5000"), null);
        assertThat(dto(List.of(parcela), null).isCronogramaValido()).isTrue();
    }

    @Test
    void aceitaContratoQuitadoInteiroNaEntrada() {
        assertThat(dto(List.of(), new BigDecimal("100000")).isCronogramaValido()).isTrue();
    }
}
