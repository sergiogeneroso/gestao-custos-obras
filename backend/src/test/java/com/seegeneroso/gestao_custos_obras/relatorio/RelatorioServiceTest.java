package com.seegeneroso.gestao_custos_obras.relatorio;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ParcelaContratoModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.DadosCasa;
import com.seegeneroso.gestao_custos_obras.imovel.DadosCompra;
import com.seegeneroso.gestao_custos_obras.imovel.DadosConstrucao;
import com.seegeneroso.gestao_custos_obras.imovel.DadosLote;
import com.seegeneroso.gestao_custos_obras.imovel.DadosVenda;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.OrcamentoCategoriaService;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CarteiraDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorM2DTO;
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

    // ADR-038: lote comprado e revendido sem obra nenhuma fecha o ciclo em LOTE — nada mais vai
    // chegar, então o resultado é definitivo. Antes, `fase != CASA` marcava esse caso para sempre.
    @Test
    void loteRevendidoSemObraNaoTemResultadoProvisorio() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        imovel.getVenda().setValor(new BigDecimal("150000"));
        imovel.getVenda().setData(LocalDate.now());
        mockar(imovel, List.of(), List.of());

        assertThat(relatorioService.resultadoImovel(1L).resultadoProvisorio()).isFalse();
    }

    // Chave nula derrubava o groupingBy por fase e devolvia 500 na tela de resultado.
    @Test
    void despesaComFaseNulaNaoDerrubaOResultado() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        DespesaModel semFase = despesa(imovel, null, new BigDecimal("2000"));
        mockar(imovel, List.of(despesa(imovel, FaseImovel.LOTE, new BigDecimal("1000")), semFase), List.of());

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.despesasPorFase()).containsOnlyKeys(FaseImovel.LOTE);
        // Fora do quadro por fase, mas dentro do total e, portanto, do custo.
        assertThat(resultado.totalDespesas()).isEqualByComparingTo("3000");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("103000");
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

    @Test
    void jurosDeParcelamentoDeVendaNaoEntramNoCusto() {
        // No PARCELAMENTO_VENDA quem paga é o comprador: o juro da parcela recebida entrou no caixa,
        // somá-lo ao custo derrubaria o lucro a cada parcela paga pelo comprador.
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        imovel.getVenda().setValor(new BigDecimal("150000"));
        imovel.getVenda().setData(LocalDate.now());
        ParcelaContratoModel recebida = parcela(new BigDecimal("8000"), new BigDecimal("500"),
                LocalDate.now(), new BigDecimal("8000"));
        ContratoFinanceiroModel contrato = contrato(TipoContratoFinanceiro.PARCELAMENTO_VENDA,
                SituacaoContrato.ATIVO, new BigDecimal("160000"), null, null, recebida);

        mockar(imovel, List.of(), List.of(contrato));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.jurosPagos()).isEqualByComparingTo("0");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("100000");
        assertThat(resultado.lucro()).isEqualByComparingTo("50000");
    }

    @Test
    void parcelamentoDeVendaContaComoAReceberNaoComoDivida() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        ParcelaContratoModel aReceber = parcela(new BigDecimal("8000"), new BigDecimal("500"), null, null);
        ContratoFinanceiroModel contratoVenda = contrato(TipoContratoFinanceiro.PARCELAMENTO_VENDA,
                SituacaoContrato.ATIVO, new BigDecimal("160000"), null, null, aReceber);
        ParcelaContratoModel aPagar = parcela(new BigDecimal("3000"), new BigDecimal("100"), null, null);
        ContratoFinanceiroModel contratoCompra = contrato(TipoContratoFinanceiro.PARCELAMENTO_COMPRA,
                SituacaoContrato.ATIVO, new BigDecimal("60000"), null, null, aPagar);

        when(imovelRepository.findByAtivoTrue()).thenReturn(List.of(imovel));
        when(despesaRepository.findByImovelIdAndAtivoTrue(anyLong())).thenReturn(List.of());
        when(contratoFinanceiroRepository.findByImovelId(anyLong())).thenReturn(List.of(contratoVenda, contratoCompra));
        when(despesaRepository.findByImovelIsNullAndAtivoTrue()).thenReturn(List.of());

        CarteiraDTO carteira = relatorioService.carteira(null, null);

        assertThat(carteira.saldoDevedorTotal()).isEqualByComparingTo("3000");
        assertThat(carteira.saldoAReceberTotal()).isEqualByComparingTo("8000");
        assertThat(carteira.parcelasAVencer30Dias()).isEqualTo(1L);
        assertThat(carteira.parcelasAReceber30Dias()).isEqualTo(1L);
        // o juro da parcela de venda também não pode inflar o custo somado na carteira
        assertThat(carteira.totalInvestido()).isEqualByComparingTo("100000");
    }

    @Test
    void custoObraPorM2UsaAreaConstruidaESoDespesasDaConstrucao() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        DespesaModel despesaLote = despesa(imovel, FaseImovel.LOTE, new BigDecimal("10000"));
        DespesaModel despesaObra = despesa(imovel, FaseImovel.CONSTRUCAO, new BigDecimal("200000"));

        mockarSemContratos(imovel, List.of(despesaLote, despesaObra));

        CustoPorM2DTO custo = relatorioService.custoPorM2(1L, null, null, null);

        // custoPorM2 mede o imóvel sobre a área do lote; custoObraPorM2 só a obra sobre o construído
        assertThat(custo.custoTotal()).isEqualByComparingTo("210000");
        assertThat(custo.custoPorM2()).isEqualByComparingTo("420.00");
        assertThat(custo.custoObra()).isEqualByComparingTo("200000");
        assertThat(custo.custoObraPorM2()).isEqualByComparingTo("2000.00");
    }

    @Test
    void custoObraPorM2ENuloSemAreaConstruida() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        imovel.getConstrucao().setArea(null);
        mockarSemContratos(imovel, List.of(despesa(imovel, FaseImovel.CONSTRUCAO, new BigDecimal("200000"))));

        CustoPorM2DTO custo = relatorioService.custoPorM2(1L, null, null, null);

        assertThat(custo.custoObra()).isEqualByComparingTo("200000");
        assertThat(custo.custoObraPorM2()).isNull();
    }

    @Test
    void diasEmCarteiraETempoDeLoteContamDaDataDaCompra() {
        // ADR-032: não existe dataInicioLote — a compra é o marco inicial do ciclo. Se alguém
        // trocar a origem dessa contagem, giro e rentabilidade passam a mentir em silêncio.
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        imovel.getCompra().setData(LocalDate.now().minusDays(200));
        imovel.getConstrucao().setDataInicio(LocalDate.now().minusDays(50));

        mockar(imovel, List.of(), List.of());

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.diasEmCarteira()).isEqualTo(200L);
        assertThat(resultado.tempoPorFase().get(FaseImovel.LOTE)).isEqualTo(150L);
        assertThat(resultado.tempoPorFase().get(FaseImovel.CONSTRUCAO)).isEqualTo(50L);
    }

    // custoPorM2 não consulta contratos — stubar findByImovelId aqui viraria UnnecessaryStubbing.
    private void mockarSemContratos(ImovelModel imovel, List<DespesaModel> despesas) {
        when(imovelRepository.findByIdAndAtivoTrue(anyLong())).thenReturn(java.util.Optional.of(imovel));
        when(despesaRepository.findByImovelIdAndAtivoTrue(anyLong())).thenReturn(despesas);
    }

    // ---- Compra parcelada do lote (ADR-037) ----------------------------------------------------
    //
    // O caso normal do negócio é entrada + parcelas SEM juros. Nele o custo do lote não pode se
    // mexer conforme as parcelas são pagas — quem anda é o desembolso.

    @Test
    void loteParceladoSemJurosNaoMudaDeCustoConformeParcelasSaoPagas() {
        // 100.000 = entrada 30.000 + 20 x 3.500. Seis parcelas pagas.
        ImovelModel imovel = imovelParcelado(1L, new BigDecimal("100000"));
        ContratoFinanceiroModel contrato = parcelamentoCompra(SituacaoContrato.ATIVO, null,
                cronograma(new BigDecimal("30000"), 20, new BigDecimal("3500"), null, 6));

        mockar(imovel, List.of(), List.of(contrato));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.custoTotal()).isEqualByComparingTo("100000");
        assertThat(resultado.jurosPagos()).isEqualByComparingTo("0");
        assertThat(resultado.totalDesembolsado()).isEqualByComparingTo("51000");
        assertThat(resultado.saldoAPagar()).isEqualByComparingTo("49000");
    }

    @Test
    void loteParceladoComJurosSoIncorporaOsJurosEfetivamentePagos() {
        // Preco a vista 100.000; entrada 30.000 + 24 x 4.000, com 1.083,33 de juros por parcela.
        ImovelModel imovel = imovelParcelado(1L, new BigDecimal("100000"));
        ContratoFinanceiroModel contrato = parcelamentoCompra(SituacaoContrato.ATIVO, null,
                cronograma(new BigDecimal("30000"), 24, new BigDecimal("4000"), new BigDecimal("1083.33"), 6));

        mockar(imovel, List.of(), List.of(contrato));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.jurosPagos()).isEqualByComparingTo("6499.98");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("106499.98");
        assertThat(resultado.totalDesembolsado()).isEqualByComparingTo("54000");
        assertThat(resultado.saldoAPagar()).isEqualByComparingTo("72000");
    }

    @Test
    void descontoNaQuitacaoDoLoteReduzOCustoEFechaComODesembolso() {
        // Sem juros: restavam 14 x 3.500 = 49.000 e foi quitado por 45.000.
        ImovelModel imovel = imovelParcelado(1L, new BigDecimal("100000"));
        ContratoFinanceiroModel contrato = parcelamentoCompra(SituacaoContrato.QUITADO, new BigDecimal("45000"),
                cronograma(new BigDecimal("30000"), 20, new BigDecimal("3500"), null, 6));

        mockar(imovel, List.of(), List.of(contrato));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.ajusteQuitacao()).isEqualByComparingTo("-4000");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("96000");
        // A invariante: custo do lote quitado = desembolso real (30.000 + 21.000 + 45.000).
        assertThat(resultado.totalDesembolsado()).isEqualByComparingTo("96000");
    }

    @Test
    void quitacaoComJurosEmbutidosSomaAoCustoEFechaComODesembolso() {
        // Restavam 18 x 4.000 = 72.000, mas so 52.500,06 disso era principal. Quitado por 66.000.
        ImovelModel imovel = imovelParcelado(1L, new BigDecimal("100000"));
        ContratoFinanceiroModel contrato = parcelamentoCompra(SituacaoContrato.QUITADO, new BigDecimal("66000"),
                cronograma(new BigDecimal("30000"), 24, new BigDecimal("4000"), new BigDecimal("1083.33"), 6));

        mockar(imovel, List.of(), List.of(contrato));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.ajusteQuitacao()).isEqualByComparingTo("13499.94");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("119999.92");
        assertThat(resultado.totalDesembolsado()).isEqualByComparingTo("120000");
    }

    @Test
    void compraAVistaContaComoDesembolsoNaDataDaCompra() {
        ImovelModel imovel = imovel(1L, new BigDecimal("80000"));

        mockar(imovel, List.of(despesa(imovel, FaseImovel.LOTE, new BigDecimal("2000"))), List.of());

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.custoTotal()).isEqualByComparingTo("82000");
        assertThat(resultado.totalDesembolsado()).isEqualByComparingTo("82000");
        assertThat(resultado.saldoAPagar()).isEqualByComparingTo("0");
        assertThat(resultado.ajusteQuitacao()).isEqualByComparingTo("0");
    }

    private ImovelModel imovelParcelado(Long id, BigDecimal valorLote) {
        ImovelModel imovel = imovel(id, valorLote);
        imovel.getCompra().setParcelada(true);
        return imovel;
    }

    private ContratoFinanceiroModel parcelamentoCompra(SituacaoContrato situacao, BigDecimal valorQuitacao,
                                                       List<ParcelaContratoModel> parcelas) {
        return ContratoFinanceiroModel.builder()
                .id(1L)
                .tipo(TipoContratoFinanceiro.PARCELAMENTO_COMPRA)
                .situacao(situacao)
                .valorContratado(new BigDecimal("100000"))
                .dataQuitacao(situacao == SituacaoContrato.QUITADO ? LocalDate.now() : null)
                .valorQuitacao(valorQuitacao)
                .parcelas(new ArrayList<>(parcelas))
                .build();
    }

    // Entrada como parcela numero 0 ja baixada, mais as prestacoes, sendo as primeiras "pagas" baixadas.
    private List<ParcelaContratoModel> cronograma(BigDecimal entrada, int quantidade, BigDecimal valorParcela,
                                                  BigDecimal jurosPorParcela, int pagas) {
        List<ParcelaContratoModel> parcelas = new ArrayList<>();
        parcelas.add(ParcelaContratoModel.builder()
                .numero(0)
                .dataVencimento(LocalDate.now().minusDays(100))
                .valor(entrada)
                .dataPagamento(LocalDate.now().minusDays(100))
                .valorPago(entrada)
                .build());

        for (int i = 1; i <= quantidade; i++) {
            boolean paga = i <= pagas;
            parcelas.add(ParcelaContratoModel.builder()
                    .numero(i)
                    .dataVencimento(LocalDate.now().plusMonths(i))
                    .valor(valorParcela)
                    .valorJuros(jurosPorParcela)
                    .dataPagamento(paga ? LocalDate.now() : null)
                    .valorPago(paga ? valorParcela : null)
                    .build());
        }
        return parcelas;
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
                .lote(DadosLote.builder().area(new BigDecimal("500")).build())
                .construcao(DadosConstrucao.builder().area(new BigDecimal("100")).build())
                .casa(new DadosCasa())
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
