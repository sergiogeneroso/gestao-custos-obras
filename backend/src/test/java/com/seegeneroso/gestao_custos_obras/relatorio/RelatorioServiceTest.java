package com.seegeneroso.gestao_custos_obras.relatorio;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ParcelaContratoModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.DadosCompra;
import com.seegeneroso.gestao_custos_obras.imovel.DadosVenda;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.OrcamentoCategoriaService;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.ResultadoImovelDTO;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

// Cobre a regra de custo da ADR-025: prestação não vira custo, saldo devedor não entra no custo,
// gasto geral não entra no custo de imóvel nenhum. Ver .agents/rules/regras-negocio-financeiras.md
// e contratos-financeiros.md.
@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private ImovelRepository imovelRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private ContratoFinanceiroRepository contratoFinanceiroRepository;
    @Mock
    private OrcamentoCategoriaService orcamentoCategoriaService;

    @InjectMocks
    private RelatorioService relatorioService;

    @Test
    void jurosDaParcelaEntramNoCustoMasAPrestacaoInteiraNao() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        ParcelaContratoModel parcelaPaga = parcela(new BigDecimal("1000"), new BigDecimal("50"),
                LocalDate.now(), new BigDecimal("1000"));
        ContratoFinanceiroModel contrato = contrato(TipoContratoFinanceiro.FINANCIAMENTO_CONSTRUCAO,
                SituacaoContrato.ATIVO, new BigDecimal("50000"), null, null, parcelaPaga);

        mockar(imovel, List.of(), List.of(contrato));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.custoTotal()).isEqualByComparingTo("100050");
        assertThat(resultado.jurosPagos()).isEqualByComparingTo("50");
    }

    @Test
    void saldoDevedorNaoEntraNoCusto() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        ParcelaContratoModel parcelaAberta = parcela(new BigDecimal("2000"), null, null, null);
        ContratoFinanceiroModel contrato = contrato(TipoContratoFinanceiro.PARCELAMENTO_COMPRA,
                SituacaoContrato.ATIVO, new BigDecimal("20000"), null, null, parcelaAberta);

        mockar(imovel, List.of(), List.of(contrato));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.custoTotal()).isEqualByComparingTo("100000");
        assertThat(resultado.contratos().get(0).saldoDevedor()).isEqualByComparingTo("2000");
    }

    @Test
    void gastoGeralNaoEntraNoCustoDoImovel() {
        // gasto geral (despesa sem imóvel) só existe em findByAtivoTrue/findByImovelIsNullAndAtivoTrue;
        // resultadoImovel só consulta findByImovelIdAndAtivoTrue, que o banco real já filtra por imóvel —
        // aqui simulamos exatamente essa lista filtrada, sem o gasto geral, para travar o comportamento.
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        DespesaModel despesaDoImovel = despesa(imovel, FaseImovel.LOTE, new BigDecimal("500"));

        mockar(imovel, List.of(despesaDoImovel), List.of());

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.custoTotal()).isEqualByComparingTo("100500");
    }

    @Test
    void custoTotalComposDespesasMultiplasFasesEParcelasPagasENaoPagas() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        DespesaModel despesaLote = despesa(imovel, FaseImovel.LOTE, new BigDecimal("2000"));
        DespesaModel despesaObra = despesa(imovel, FaseImovel.CONSTRUCAO, new BigDecimal("200000"));
        ParcelaContratoModel paga = parcela(new BigDecimal("1000"), new BigDecimal("30"), LocalDate.now(), new BigDecimal("1000"));
        ParcelaContratoModel aberta = parcela(new BigDecimal("1000"), new BigDecimal("30"), null, null);
        ContratoFinanceiroModel contrato = contrato(TipoContratoFinanceiro.FINANCIAMENTO_CONSTRUCAO,
                SituacaoContrato.ATIVO, new BigDecimal("200000"), null, null, paga, aberta);

        mockar(imovel, List.of(despesaLote, despesaObra), List.of(contrato));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.despesasPorFase().get(FaseImovel.LOTE)).isEqualByComparingTo("2000");
        assertThat(resultado.despesasPorFase().get(FaseImovel.CONSTRUCAO)).isEqualByComparingTo("200000");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("302030");
    }

    @Test
    void semContratoCustoEApenasCompraMaisDespesas() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        DespesaModel despesa = despesa(imovel, FaseImovel.LOTE, new BigDecimal("3000"));

        mockar(imovel, List.of(despesa), List.of());

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.custoTotal()).isEqualByComparingTo("103000");
        assertThat(resultado.contratos()).isEmpty();
    }

    @Test
    void lucroEMargemSoCalculadosQuandoVendido() {
        ImovelModel adquirido = imovel(1L, new BigDecimal("100000"));
        mockar(adquirido, List.of(), List.of());
        assertThat(relatorioService.resultadoImovel(1L).lucro()).isNull();

        ImovelModel vendido = imovel(1L, new BigDecimal("100000"));
        vendido.setSituacao(SituacaoImovel.VENDIDO);
        vendido.setFase(FaseImovel.CASA);
        vendido.getVenda().setValor(new BigDecimal("150000"));
        vendido.getVenda().setData(LocalDate.now());
        mockar(vendido, List.of(), List.of());

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);
        assertThat(resultado.lucro()).isEqualByComparingTo("50000");
        assertThat(resultado.margem()).isEqualByComparingTo(new BigDecimal("50000").divide(new BigDecimal("150000"), 4, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void resultadoProvisorioQuandoVendidoComObraPendente() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        imovel.setFase(FaseImovel.CONSTRUCAO);
        imovel.getVenda().setValor(new BigDecimal("150000"));
        imovel.getVenda().setData(LocalDate.now());
        mockar(imovel, List.of(), List.of());

        assertThat(relatorioService.resultadoImovel(1L).resultadoProvisorio()).isTrue();
    }

    @Test
    void quitacaoEntraNoTotalPagoNuncaNoCustoESaldoDevedorZera() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        ParcelaContratoModel aberta = parcela(new BigDecimal("5000"), new BigDecimal("100"), null, null);
        ContratoFinanceiroModel contrato = contrato(TipoContratoFinanceiro.FINANCIAMENTO_CONSTRUCAO,
                SituacaoContrato.QUITADO, new BigDecimal("200000"), LocalDate.now(), new BigDecimal("205000"), aberta);

        mockar(imovel, List.of(), List.of(contrato));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.custoTotal()).isEqualByComparingTo("100000");
        assertThat(resultado.contratos().get(0).totalPago()).isEqualByComparingTo("205000");
        assertThat(resultado.contratos().get(0).saldoDevedor()).isEqualByComparingTo("0");
    }

    private void mockar(ImovelModel imovel, List<DespesaModel> despesas, List<ContratoFinanceiroModel> contratos) {
        when(imovelRepository.findByIdAndAtivoTrue(anyLong())).thenReturn(java.util.Optional.of(imovel));
        when(despesaRepository.findByImovelIdAndAtivoTrue(anyLong())).thenReturn(despesas);
        when(contratoFinanceiroRepository.findByImovelId(anyLong())).thenReturn(contratos);
    }

    private ImovelModel imovel(Long id, BigDecimal valorCompra) {
        return ImovelModel.builder()
                .id(id)
                .identificador("Lote " + id)
                .fase(FaseImovel.LOTE)
                .situacao(SituacaoImovel.ADQUIRIDO)
                .dataInicioLote(LocalDate.now().minusDays(100))
                .compra(DadosCompra.builder().valor(valorCompra).data(LocalDate.now().minusDays(100)).build())
                .venda(new DadosVenda())
                .ativo(true)
                .build();
    }

    private DespesaModel despesa(ImovelModel imovel, FaseImovel fase, BigDecimal valor) {
        return DespesaModel.builder()
                .imovel(imovel)
                .faseImovel(fase)
                .valor(valor)
                .dataPagamento(LocalDate.now())
                .ativo(true)
                .build();
    }

    private ContratoFinanceiroModel contrato(TipoContratoFinanceiro tipo, SituacaoContrato situacao,
                                             BigDecimal valorContratado, LocalDate dataQuitacao,
                                             BigDecimal valorQuitacao, ParcelaContratoModel... parcelas) {
        ContratoFinanceiroModel contrato = ContratoFinanceiroModel.builder()
                .id(1L)
                .tipo(tipo)
                .situacao(situacao)
                .valorContratado(valorContratado)
                .dataQuitacao(dataQuitacao)
                .valorQuitacao(valorQuitacao)
                .parcelas(new ArrayList<>(List.of(parcelas)))
                .build();
        return contrato;
    }

    private ParcelaContratoModel parcela(BigDecimal valor, BigDecimal valorJuros, LocalDate dataPagamento, BigDecimal valorPago) {
        return ParcelaContratoModel.builder()
                .numero(1)
                .dataVencimento(LocalDate.now())
                .valor(valor)
                .valorJuros(valorJuros)
                .dataPagamento(dataPagamento)
                .valorPago(valorPago)
                .build();
    }
}
