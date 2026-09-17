package com.seegeneroso.gestao_custos_obras.imovel.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ImovelRequestDTOTest {

    private ImovelRequestDTO dto(BigDecimal compraValor, Boolean compraParcelada) {
        return new ImovelRequestDTO(
                "L001", "Rua A", "10", "Bairro", "Cidade", "MG", "12345-000", null,
                null, null, null,
                compraValor, LocalDate.now(), 1L, compraParcelada, null, null
        );
    }

    @Test
    void exigeValorDaCompraQuandoNaoParcelada() {
        assertThat(dto(null, false).isCompraValorValido()).isFalse();
        assertThat(dto(null, null).isCompraValorValido()).isFalse();
    }

    @Test
    void dispensaValorDaCompraQuandoParcelada() {
        assertThat(dto(null, true).isCompraValorValido()).isTrue();
    }

    @Test
    void aceitaValorDaCompraInformadoIndependenteDeParcelada() {
        assertThat(dto(new BigDecimal("100000"), false).isCompraValorValido()).isTrue();
        assertThat(dto(new BigDecimal("100000"), true).isCompraValorValido()).isTrue();
    }
}
