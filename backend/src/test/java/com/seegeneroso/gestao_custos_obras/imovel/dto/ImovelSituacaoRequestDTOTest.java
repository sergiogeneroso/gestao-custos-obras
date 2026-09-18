package com.seegeneroso.gestao_custos_obras.imovel.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ImovelSituacaoRequestDTOTest {

    @Test
    void naoExigeNadaQuandoNaoEVendido() {
        var dto = new ImovelSituacaoRequestDTO(SituacaoImovel.A_VENDA, null, null, null, null, null);
        assertThat(dto.isVendaValida()).isTrue();
    }

    @Test
    void exigeValorDataECompradorQuandoVendido() {
        var semNada = new ImovelSituacaoRequestDTO(SituacaoImovel.VENDIDO, null, null, null, null, null);
        var semComprador = new ImovelSituacaoRequestDTO(
                SituacaoImovel.VENDIDO, new BigDecimal("300000"), LocalDate.now(), null, null, null);
        assertThat(semNada.isVendaValida()).isFalse();
        assertThat(semComprador.isVendaValida()).isFalse();
    }

    @Test
    void aceitaVendidoComValorDataEComprador() {
        var dto = new ImovelSituacaoRequestDTO(
                SituacaoImovel.VENDIDO, new BigDecimal("300000"), LocalDate.now(), 1L, null, null);
        assertThat(dto.isVendaValida()).isTrue();
    }
}
