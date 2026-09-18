package com.seegeneroso.gestao_custos_obras.relatorio;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
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
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorImovelDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorM2DTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.ResultadoImovelDTO;
import com.seegeneroso.gestao_custos_obras.shared.enums.EtapaConstrucao;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;
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
    void custoSemCompraSomaDespesasDeTodasAsFasesMaisJurosPagosSemTocarNoCustoTotal() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        DespesaModel despesaLote = despesa(imovel, FaseImovel.LOTE, new BigDecimal("2000"));
        DespesaModel despesaObra = despesa(imovel, FaseImovel.CONSTRUCAO, new BigDecimal("200000"));
        DespesaModel despesaCasa = despesa(imovel, FaseImovel.CASA, new BigDecimal("5000"));
        ParcelaContratoModel paga = parcela(new BigDecimal("1000"), new BigDecimal("30"), LocalDate.now(), new BigDecimal("1000"));
        ContratoFinanceiroModel contrato = contrato(TipoContratoFinanceiro.FINANCIAMENTO_CONSTRUCAO,
                SituacaoContrato.ATIVO, new BigDecimal("200000"), null, null, paga);

        mockar(imovel, List.of(despesaLote, despesaObra, despesaCasa), List.of(contrato));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        // 2000 + 200000 + 5000 de despesas + 30 de juros pagos, sem os 100000 da compra.
        assertThat(resultado.custoSemCompra()).isEqualByComparingTo("207030");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("307030");
        assertThat(resultado.custoTotal().subtract(resultado.custoSemCompra())).isEqualByComparingTo("100000");
    }

    // O ajuste da quitação antecipada é preço do lote (desconto ou juros embutidos no valor
    // negociado), então ele fica fora do indicador junto com a compra — senão voltaria por outra
    // porta exatamente o que a exclusão da compra deveria tirar.
    @Test
    void custoSemCompraIgnoraOAjusteDeQuitacaoQueEPrecoDoLote() {
        ImovelModel imovel = imovelParcelado(1L, new BigDecimal("100000"));
        DespesaModel despesaObra = despesa(imovel, FaseImovel.CONSTRUCAO, new BigDecimal("50000"));
        ParcelaContratoModel aberta = parcela(new BigDecimal("20000"), null, null, null);
        ContratoFinanceiroModel quitado = contrato(TipoContratoFinanceiro.PARCELAMENTO_COMPRA,
                SituacaoContrato.QUITADO, new BigDecimal("100000"), LocalDate.now(), new BigDecimal("18000"), aberta);

        mockar(imovel, List.of(despesaObra), List.of(quitado));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.ajusteQuitacao()).isEqualByComparingTo("-2000");
        assertThat(resultado.custoSemCompra()).isEqualByComparingTo("50000");
        assertThat(resultado.custoTotal()).isEqualByComparingTo("148000");
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

    // rentabilidadeAnualizada anualiza o ROI pelo tempo em carteira (Math.pow, exceção documentada
    // à proibição de double para valor monetário — este é indicador percentual).
    @Test
    void rentabilidadeAnualizadaAnualizaOLucroPeloTempoEmCarteira() {
        // Compra em 2025-01-01, venda em 2027-01-01: 730 dias corridos (2025 e 2026 não são bissextos).
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        imovel.getCompra().setData(LocalDate.of(2025, 1, 1));
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        imovel.setFase(FaseImovel.CASA);
        imovel.getVenda().setValor(new BigDecimal("150000"));
        imovel.getVenda().setData(LocalDate.of(2027, 1, 1));

        mockar(imovel, List.of(), List.of());

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.diasEmCarteira()).isEqualTo(730L);
        // roi = 50000/100000 = 0,5 em dois anos; anualizado = 1,5^(365/730) - 1 = 0,224745
        assertThat(resultado.rentabilidadeAnualizada()).isCloseTo(0.224745, offset(0.0001));
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

    // ADR-039/regras-negocio-financeiras.md: despesasPorEtapa é recorte de apresentação sobre o
    // mesmo dinheiro que já entra por fase — não pode dobrar o custo, e despesa sem etapa fica de
    // fora do quadro em vez de virar chave nula (mesmo espírito de despesaComFaseNulaNaoDerrubaOResultado).
    @Test
    void despesasPorEtapaNuncaEntraNoCustoEDespesaSemEtapaFicaForaDoQuadro() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        DespesaModel comEtapa = despesaComEtapa(imovel, FaseImovel.CONSTRUCAO, EtapaConstrucao.FUNDACAO, new BigDecimal("20000"));
        DespesaModel semEtapa = despesaComEtapa(imovel, FaseImovel.CONSTRUCAO, null, new BigDecimal("5000"));

        mockar(imovel, List.of(comEtapa, semEtapa), List.of());

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.despesasPorEtapa()).containsOnlyKeys(EtapaConstrucao.FUNDACAO);
        assertThat(resultado.despesasPorEtapa().get(EtapaConstrucao.FUNDACAO)).isEqualByComparingTo("20000");
        // As duas despesas entram no custo normalmente — o quadro por etapa é só apresentação.
        assertThat(resultado.custoTotal()).isEqualByComparingTo("125000");
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

    // contratos-financeiros.md: receber parcela de PARCELAMENTO_VENDA é caixa entrando, nunca
    // receita nova — a receita é o valor da venda, já registrado em venda.valor. O que já entrou
    // fica visível só na posição do contrato (totalPago), não em lucro/custoTotal/totalDesembolsado.
    @Test
    void parcelaRecebidaDeVendaParceladaECaixaNaoAlteraLucroNemDesembolso() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        imovel.getVenda().setValor(new BigDecimal("150000"));
        imovel.getVenda().setData(LocalDate.now());
        ParcelaContratoModel entradaPaga = parcela(new BigDecimal("50000"), null, LocalDate.now(), new BigDecimal("50000"));
        ParcelaContratoModel parcelaPaga = parcela(new BigDecimal("20000"), null, LocalDate.now(), new BigDecimal("20000"));
        ContratoFinanceiroModel contratoVenda = contrato(TipoContratoFinanceiro.PARCELAMENTO_VENDA,
                SituacaoContrato.ATIVO, new BigDecimal("150000"), null, null, entradaPaga, parcelaPaga);

        mockar(imovel, List.of(), List.of(contratoVenda));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.custoTotal()).isEqualByComparingTo("100000");
        assertThat(resultado.lucro()).isEqualByComparingTo("50000");
        assertThat(resultado.totalDesembolsado()).isEqualByComparingTo("100000");
        // As 70.000 recebidas do comprador (50.000 + 20.000) aparecem como caixa do contrato, não
        // como receita extra somada ao lucro.
        assertThat(resultado.contratos().get(0).totalPago()).isEqualByComparingTo("70000");
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

    // ADR-043: contrato de venda cancelado não é mais "a receber" (o negócio caiu, ninguém deve o
    // resto) nem "saldo devedor" — o que falta é estorno do que já foi pago.
    @Test
    void vendaDesfeitaComParcelaPagaContaComoSaldoAEstornarNaoComoAReceber() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        ParcelaContratoModel paga = parcela(new BigDecimal("8000"), null, LocalDate.now(), new BigDecimal("8000"));
        ContratoFinanceiroModel contratoCancelado = contrato(TipoContratoFinanceiro.PARCELAMENTO_VENDA,
                SituacaoContrato.CANCELADO, new BigDecimal("160000"), null, null, paga);
        contratoCancelado.setValorEstornado(new BigDecimal("3000"));

        when(imovelRepository.findByAtivoTrue()).thenReturn(List.of(imovel));
        when(despesaRepository.findByImovelIdAndAtivoTrue(anyLong())).thenReturn(List.of());
        when(contratoFinanceiroRepository.findByImovelId(anyLong())).thenReturn(List.of(contratoCancelado));
        when(despesaRepository.findByImovelIsNullAndAtivoTrue()).thenReturn(List.of());

        CarteiraDTO carteira = relatorioService.carteira(null, null);

        assertThat(carteira.saldoAEstornarTotal()).isEqualByComparingTo("5000");
        assertThat(carteira.saldoAReceberTotal()).isEqualByComparingTo("0");
        assertThat(carteira.saldoDevedorTotal()).isEqualByComparingTo("0");
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

    // Obra concluída soma tempo também em CONSTRUCAO (início -> conclusão) e CASA (conclusão ->
    // venda, ou hoje se ainda não vendido) — não só o tempo de LOTE já coberto acima.
    @Test
    void tempoPorFaseComObraConcluidaContabilizaConstrucaoECasa() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        LocalDate compraData = LocalDate.of(2026, 1, 10);
        imovel.getCompra().setData(compraData);
        imovel.getConstrucao().setDataInicio(compraData.plusDays(30));
        imovel.getCasa().setDataConclusaoObra(compraData.plusDays(230));
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        imovel.setFase(FaseImovel.CASA);
        imovel.getVenda().setValor(new BigDecimal("150000"));
        imovel.getVenda().setData(compraData.plusDays(275));

        mockar(imovel, List.of(), List.of());

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        assertThat(resultado.tempoPorFase().get(FaseImovel.LOTE)).isEqualTo(30L);
        assertThat(resultado.tempoPorFase().get(FaseImovel.CONSTRUCAO)).isEqualTo(200L);
        assertThat(resultado.tempoPorFase().get(FaseImovel.CASA)).isEqualTo(45L);
    }

    @Test
    void custoPorM2SemImovelIdLancaRegraDeNegocio() {
        assertThatThrownBy(() -> relatorioService.custoPorM2(null, null, null, null))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void custoPorImovelFiltraPorCategoriaSomandoSoAsDespesasDaquelaCategoria() {
        ImovelModel imovel1 = imovel(1L, new BigDecimal("100000"));
        ImovelModel imovel2 = imovel(2L, new BigDecimal("50000"));
        CategoriaDespesaModel material = categoria(10L);
        CategoriaDespesaModel maoDeObra = categoria(20L);
        DespesaModel materialImovel1 = despesaComCategoria(imovel1, material, new BigDecimal("2000"));
        DespesaModel maoDeObraImovel1 = despesaComCategoria(imovel1, maoDeObra, new BigDecimal("3000"));
        DespesaModel materialImovel2 = despesaComCategoria(imovel2, material, new BigDecimal("1000"));

        when(imovelRepository.findByAtivoTrue()).thenReturn(List.of(imovel1, imovel2));
        when(despesaRepository.findByImovelIdAndAtivoTrue(1L)).thenReturn(List.of(materialImovel1, maoDeObraImovel1));
        when(despesaRepository.findByImovelIdAndAtivoTrue(2L)).thenReturn(List.of(materialImovel2));

        List<CustoPorImovelDTO> resultado = relatorioService.custoPorImovel(null, 10L, null, null);

        assertThat(resultado).hasSize(2);
        // maoDeObraImovel1 (categoria 20) fica de fora do custo do imóvel 1.
        assertThat(resultado.get(0).custoTotal()).isEqualByComparingTo("2000");
        assertThat(resultado.get(1).custoTotal()).isEqualByComparingTo("1000");
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

    // Lacuna fechada: um mesmo imóvel encadeia PARCELAMENTO_COMPRA quitado antecipadamente com
    // FINANCIAMENTO_CONSTRUCAO ativo (contratos-financeiros.md) — os dois precisam somar certo no
    // mesmo resultadoImovel, não só isolados como nos testes acima.
    @Test
    void parcelamentoCompraQuitadoMaisFinanciamentoConstrucaoAtivoSomamNoMesmoResultado() {
        ImovelModel imovel = imovelParcelado(1L, new BigDecimal("100000"));
        // Lote: entrada 30.000 + 20 x 3.500 = 100.000, sem juros; quitado por 45.000 com 6 parcelas já pagas.
        ContratoFinanceiroModel lote = parcelamentoCompra(SituacaoContrato.QUITADO, new BigDecimal("45000"),
                cronograma(new BigDecimal("30000"), 20, new BigDecimal("3500"), null, 6));
        // Financiamento da obra: uma parcela paga com 200 de juros e uma parcela em aberto.
        ParcelaContratoModel paga = parcela(new BigDecimal("5000"), new BigDecimal("200"), LocalDate.now(), new BigDecimal("5000"));
        ParcelaContratoModel aberta = parcela(new BigDecimal("6000"), new BigDecimal("300"), null, null);
        ContratoFinanceiroModel financiamento = contrato(TipoContratoFinanceiro.FINANCIAMENTO_CONSTRUCAO,
                SituacaoContrato.ATIVO, new BigDecimal("100000"), null, null, paga, aberta);

        mockar(imovel, List.of(), List.of(lote, financiamento));

        ResultadoImovelDTO resultado = relatorioService.resultadoImovel(1L);

        // jurosPagos = 200: só a parcela paga do financiamento tem valorJuros; o lote não tem juros.
        assertThat(resultado.jurosPagos()).isEqualByComparingTo("200");
        // ajusteQuitacao do lote: principal em aberto = 14 parcelas x 3.500 = 49.000; 45.000 - 49.000 = -4.000.
        assertThat(resultado.ajusteQuitacao()).isEqualByComparingTo("-4000");
        // custoTotal = compra (100000) + despesas (0) + jurosPagos (200) + ajusteQuitacao (-4000) = 96.200.
        assertThat(resultado.custoTotal()).isEqualByComparingTo("96200");
        // totalDesembolsado: lote pago = entrada 30.000 + 6 x 3.500 = 51.000, mais os 45.000 da
        // quitação = 96.000; financiamento pago = só a parcela paga, 5.000. Total = 101.000.
        assertThat(resultado.totalDesembolsado()).isEqualByComparingTo("101000");
        // saldoAPagar: lote quitado conta zero; financiamento em aberto = 6.000 (a parcela aberta).
        assertThat(resultado.saldoAPagar()).isEqualByComparingTo("6000");
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
                .build();
    }

    private DespesaModel despesa(ImovelModel imovel, FaseImovel fase, BigDecimal valor) {
        return DespesaModel.builder()
                .imovel(imovel)
                .faseImovel(fase)
                .valor(valor)
                .dataPagamento(LocalDate.now())
                .build();
    }

    private DespesaModel despesaComEtapa(ImovelModel imovel, FaseImovel fase, EtapaConstrucao etapa, BigDecimal valor) {
        return DespesaModel.builder()
                .imovel(imovel)
                .faseImovel(fase)
                .etapaConstrucao(etapa)
                .valor(valor)
                .dataPagamento(LocalDate.now())
                .build();
    }

    private DespesaModel despesaComCategoria(ImovelModel imovel, CategoriaDespesaModel categoria, BigDecimal valor) {
        return DespesaModel.builder()
                .imovel(imovel)
                .categoriaDespesa(categoria)
                .valor(valor)
                .dataPagamento(LocalDate.now())
                .build();
    }

    private CategoriaDespesaModel categoria(Long id) {
        return CategoriaDespesaModel.builder().id(id).nome("Categoria " + id).build();
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
