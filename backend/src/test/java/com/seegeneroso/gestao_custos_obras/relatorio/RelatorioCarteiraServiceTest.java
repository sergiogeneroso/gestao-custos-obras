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
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto.OrcamentoCategoriaResponseDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CarteiraDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.ExtratoPessoaDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.OrcadoVsRealizadoDTO;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

// Continuação de RelatorioServiceTest (separado por tamanho de arquivo, decisão mecânica —
// .scratch/cobertura-testes-financeiro/issues/03-relatorio.md): carteira, extratos por
// pagador/beneficiário e orçado vs realizado. Mesma regra de custo da ADR-025.
@ExtendWith(MockitoExtension.class)
class RelatorioCarteiraServiceTest {

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
    void orcadoVsRealizadoSomaOrcadoRealizadoEDiferencaPorCategoria() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(orcamentoCategoriaService.listar(1L)).thenReturn(List.of(
                orcamentoCategoria(new BigDecimal("10000"), new BigDecimal("6000")),
                orcamentoCategoria(new BigDecimal("5000"), new BigDecimal("7000"))
        ));

        OrcadoVsRealizadoDTO resultado = relatorioService.orcadoVsRealizado(1L);

        assertThat(resultado.valorOrcadoTotal()).isEqualByComparingTo("15000");
        assertThat(resultado.valorRealizadoTotal()).isEqualByComparingTo("13000");
        assertThat(resultado.diferenca()).isEqualByComparingTo("2000");
    }

    @Test
    void orcadoVsRealizadoSemImovelIdLancaRegraDeNegocio() {
        assertThatThrownBy(() -> relatorioService.orcadoVsRealizado(null))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void extratoPessoasSomaTotalPorPagador() {
        PessoaModel joao = pessoa(1L, "João");
        PessoaModel maria = pessoa(2L, "Maria");
        DespesaModel pagaJoaoSemBeneficiario = despesaComPessoas(joao, null, new BigDecimal("1000"));
        DespesaModel pagaJoaoRecebeMaria = despesaComPessoas(joao, maria, new BigDecimal("2000"));
        DespesaModel pagaMariaRecebeJoao = despesaComPessoas(maria, joao, new BigDecimal("500"));

        when(pessoaRepository.findByAtivoTrue()).thenReturn(List.of(joao, maria));
        when(despesaRepository.findByAtivoTrue())
                .thenReturn(List.of(pagaJoaoSemBeneficiario, pagaJoaoRecebeMaria, pagaMariaRecebeJoao));

        List<ExtratoPessoaDTO> extrato = relatorioService.extratoPessoas(null, null, null, null, null);

        assertThat(extrato).containsExactlyInAnyOrder(
                new ExtratoPessoaDTO(1L, "João", new BigDecimal("3000")), // 1000 + 2000 pagos por João
                new ExtratoPessoaDTO(2L, "Maria", new BigDecimal("500"))
        );
    }

    @Test
    void historicoFornecedorSomaTotalPorBeneficiario() {
        PessoaModel joao = pessoa(1L, "João");
        PessoaModel maria = pessoa(2L, "Maria");
        DespesaModel pagaJoaoSemBeneficiario = despesaComPessoas(joao, null, new BigDecimal("1000"));
        DespesaModel pagaJoaoRecebeMaria = despesaComPessoas(joao, maria, new BigDecimal("2000"));
        DespesaModel pagaMariaRecebeJoao = despesaComPessoas(maria, joao, new BigDecimal("500"));

        when(pessoaRepository.findByAtivoTrue()).thenReturn(List.of(joao, maria));
        when(despesaRepository.findByAtivoTrue())
                .thenReturn(List.of(pagaJoaoSemBeneficiario, pagaJoaoRecebeMaria, pagaMariaRecebeJoao));

        List<ExtratoPessoaDTO> historico = relatorioService.historicoFornecedor(null, null, null, null, null);

        // pagaJoaoSemBeneficiario não conta para ninguém como beneficiário.
        assertThat(historico).containsExactlyInAnyOrder(
                new ExtratoPessoaDTO(2L, "Maria", new BigDecimal("2000")),
                new ExtratoPessoaDTO(1L, "João", new BigDecimal("500"))
        );
    }

    @Test
    void carteiraSomaTotalVendidoLucroRealizadoETotalGastoSemComprasEContaImoveisPorFaseESituacao() {
        ImovelModel adquirido = imovel(1L, new BigDecimal("100000"));
        ImovelModel vendido = imovel(2L, new BigDecimal("80000"));
        vendido.setSituacao(SituacaoImovel.VENDIDO);
        vendido.setFase(FaseImovel.CASA);
        vendido.getVenda().setValor(new BigDecimal("150000"));
        vendido.getVenda().setData(LocalDate.now());
        DespesaModel despesaVendido = despesa(vendido, new BigDecimal("5000"));

        when(imovelRepository.findByAtivoTrue()).thenReturn(List.of(adquirido, vendido));
        when(despesaRepository.findByImovelIdAndAtivoTrue(1L)).thenReturn(List.of());
        when(despesaRepository.findByImovelIdAndAtivoTrue(2L)).thenReturn(List.of(despesaVendido));
        when(contratoFinanceiroRepository.findByImovelId(anyLong())).thenReturn(List.of());
        when(despesaRepository.findByImovelIsNullAndAtivoTrue()).thenReturn(List.of());

        CarteiraDTO carteira = relatorioService.carteira(null, null);

        // custoTotal do vendido = 80000 + 5000 = 85000; lucro = 150000 - 85000.
        assertThat(carteira.totalVendido()).isEqualByComparingTo("150000");
        assertThat(carteira.lucroRealizado()).isEqualByComparingTo("65000");
        assertThat(carteira.totalInvestido()).isEqualByComparingTo("185000");
        // totalGastoSemCompras ignora as duas compras (100000 e 80000): só os 5000 de despesa.
        assertThat(carteira.totalGastoSemCompras()).isEqualByComparingTo("5000");
        assertThat(carteira.imoveisPorFase()).containsEntry(FaseImovel.LOTE, 1L).containsEntry(FaseImovel.CASA, 1L);
        assertThat(carteira.imoveisPorSituacao())
                .containsEntry(SituacaoImovel.ADQUIRIDO, 1L)
                .containsEntry(SituacaoImovel.VENDIDO, 1L);
    }

    @Test
    void gastosGeraisPeriodoSomaSoDespesasSemImovelDentroDoFiltroDeData() {
        DespesaModel dentroDoFiltro = despesaGeral(new BigDecimal("1000"), LocalDate.of(2026, 1, 10));
        DespesaModel foraDoFiltro = despesaGeral(new BigDecimal("2000"), LocalDate.of(2026, 3, 10));

        when(imovelRepository.findByAtivoTrue()).thenReturn(List.of());
        when(despesaRepository.findByImovelIsNullAndAtivoTrue()).thenReturn(List.of(dentroDoFiltro, foraDoFiltro));

        CarteiraDTO carteira = relatorioService.carteira(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(carteira.gastosGeraisPeriodo()).isEqualByComparingTo("1000");
    }

    // ADR-043: contrato de venda cancelado é estorno pendente, não dívida nem a receber — e não
    // pode inflar totalInvestido/lucroRealizado, que continuam saindo só do custoTotal do imóvel.
    @Test
    void contratoCanceladoNaoEntraEmTotalInvestidoNemLucroRealizadoMasEntraEmSaldoAEstornar() {
        ImovelModel imovel = imovel(1L, new BigDecimal("100000"));
        ParcelaContratoModel paga = parcela(new BigDecimal("8000"), LocalDate.now(), new BigDecimal("8000"));
        ContratoFinanceiroModel cancelado = ContratoFinanceiroModel.builder()
                .id(1L)
                .tipo(TipoContratoFinanceiro.PARCELAMENTO_VENDA)
                .situacao(SituacaoContrato.CANCELADO)
                .valorContratado(new BigDecimal("160000"))
                .valorEstornado(new BigDecimal("3000"))
                .parcelas(new ArrayList<>(List.of(paga)))
                .build();

        when(imovelRepository.findByAtivoTrue()).thenReturn(List.of(imovel));
        when(despesaRepository.findByImovelIdAndAtivoTrue(anyLong())).thenReturn(List.of());
        when(contratoFinanceiroRepository.findByImovelId(anyLong())).thenReturn(List.of(cancelado));
        when(despesaRepository.findByImovelIsNullAndAtivoTrue()).thenReturn(List.of());

        CarteiraDTO carteira = relatorioService.carteira(null, null);

        assertThat(carteira.totalInvestido()).isEqualByComparingTo("100000");
        assertThat(carteira.lucroRealizado()).isEqualByComparingTo("0"); // imóvel não está VENDIDO
        assertThat(carteira.saldoAEstornarTotal()).isEqualByComparingTo("5000"); // 8000 pago - 3000 já estornado
        assertThat(carteira.saldoDevedorTotal()).isEqualByComparingTo("0");
        assertThat(carteira.saldoAReceberTotal()).isEqualByComparingTo("0");
    }

    private OrcamentoCategoriaResponseDTO orcamentoCategoria(BigDecimal valorOrcado, BigDecimal totalGasto) {
        return new OrcamentoCategoriaResponseDTO(1L, 1L, "Lote 1", 1L, "Categoria",
                valorOrcado, null, null, totalGasto, valorOrcado.subtract(totalGasto), "EM_ANDAMENTO");
    }

    private PessoaModel pessoa(Long id, String nome) {
        return PessoaModel.builder().id(id).nome(nome).build();
    }

    private DespesaModel despesaComPessoas(PessoaModel pagador, PessoaModel beneficiario, BigDecimal valor) {
        return DespesaModel.builder()
                .pagador(pagador)
                .beneficiario(beneficiario)
                .valor(valor)
                .dataPagamento(LocalDate.now())
                .build();
    }

    private DespesaModel despesa(ImovelModel imovel, BigDecimal valor) {
        return DespesaModel.builder()
                .imovel(imovel)
                .valor(valor)
                .dataPagamento(LocalDate.now())
                .build();
    }

    private DespesaModel despesaGeral(BigDecimal valor, LocalDate dataPagamento) {
        return DespesaModel.builder()
                .imovel(null)
                .valor(valor)
                .dataPagamento(dataPagamento)
                .build();
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

    private ParcelaContratoModel parcela(BigDecimal valor, LocalDate dataPagamento, BigDecimal valorPago) {
        return ParcelaContratoModel.builder()
                .numero(1)
                .dataVencimento(LocalDate.now())
                .valor(valor)
                .dataPagamento(dataPagamento)
                .valorPago(valorPago)
                .build();
    }
}
