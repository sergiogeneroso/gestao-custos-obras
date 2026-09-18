package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoEstornoRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ParcelaContratoRequestDTO;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.DadosCompra;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import com.seegeneroso.gestao_custos_obras.shared.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cobre a ADR-037: entrada como parcela numero 0 ja baixada e gravacao do valor do lote a partir do
// cronograma. Ver .agents/rules/contratos-financeiros.md.
@ExtendWith(MockitoExtension.class)
class ContratoFinanceiroServiceTest {

    @Mock
    private ContratoFinanceiroRepository contratoFinanceiroRepository;
    @Mock
    private ParcelaContratoRepository parcelaContratoRepository;
    @Mock
    private ImovelRepository imovelRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private ContratoDocumentoRepository contratoDocumentoRepository;
    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private ContratoFinanceiroMapper contratoFinanceiroMapper;
    @Mock
    private UsuarioAutenticadoService usuarioAutenticadoService;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ContratoFinanceiroService service;

    @Test
    void entradaViraParcelaZeroJaBaixadaNaDataInformada() {
        ImovelModel imovel = imovel(null);
        mockar(imovel);

        service.criar(requisicao(imovel, new BigDecimal("20000"), null));

        ParcelaContratoModel entrada = capturarParcela(0);
        assertThat(entrada).isNotNull();
        assertThat(entrada.getValor()).isEqualByComparingTo("20000");
        assertThat(entrada.getValorPago()).isEqualByComparingTo("20000");
        assertThat(entrada.getDataPagamento()).isEqualTo(LocalDate.of(2026, 8, 15));
        // As prestacoes continuam em aberto.
        assertThat(capturarParcela(1).getDataPagamento()).isNull();
    }

    @Test
    void valorDoLoteEDeduzidoDoCronogramaQuandoNaoInformado() {
        // Entrada 20.000 + 6 x 5.000 = 50.000, o caso normal (sem juros).
        ImovelModel imovel = imovel(null);
        mockar(imovel);

        service.criar(requisicao(imovel, new BigDecimal("20000"), null));

        assertThat(imovel.getCompra().getValor()).isEqualByComparingTo("50000");
        verify(imovelRepository).save(imovel);
    }

    @Test
    void precoAVistaInformadoPrevaleceSobreOTotalDoCronograma() {
        ImovelModel imovel = imovel(null);
        mockar(imovel);

        service.criar(requisicao(imovel, new BigDecimal("20000"), new BigDecimal("47000")));

        assertThat(imovel.getCompra().getValor()).isEqualByComparingTo("47000");
    }

    @Test
    void valorDoLoteJaPreenchidoNuncaEhSobrescrito() {
        ImovelModel imovel = imovel(new BigDecimal("80000"));
        mockar(imovel);

        service.criar(requisicao(imovel, new BigDecimal("20000"), new BigDecimal("47000")));

        assertThat(imovel.getCompra().getValor()).isEqualByComparingTo("80000");
        verify(imovelRepository, never()).save(any(ImovelModel.class));
    }

    @Test
    void financiamentoDeConstrucaoNaoMexeNoValorDoLote() {
        ImovelModel imovel = imovel(null);
        mockar(imovel);

        ContratoFinanceiroRequestDTO dto = new ContratoFinanceiroRequestDTO(
                1L, TipoContratoFinanceiro.FINANCIAMENTO_CONSTRUCAO, 1L, new BigDecimal("200000"),
                List.of(new ParcelaContratoRequestDTO(1, LocalDate.of(2026, 9, 15), new BigDecimal("5000"), null)),
                null, null, null);

        service.criar(dto);

        assertThat(imovel.getCompra().getValor()).isNull();
        verify(imovelRepository, never()).save(any(ImovelModel.class));
    }

    @Test
    void excluirRecusaContratoQuitado() {
        ContratoFinanceiroModel contrato = contratoParaExcluir(SituacaoContrato.QUITADO);
        when(contratoFinanceiroRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(contrato));

        assertThrows(RegraDeNegocioException.class, () -> service.excluir(1L, "cadastro errado"));
        verify(contratoFinanceiroRepository, never()).save(any());
    }

    @Test
    void excluirRecusaContratoComParcelaPaga() {
        ContratoFinanceiroModel contrato = contratoParaExcluir(SituacaoContrato.ATIVO);
        contrato.getParcelas().get(0).setDataPagamento(LocalDate.of(2026, 9, 15));
        when(contratoFinanceiroRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(contrato));

        assertThrows(RegraDeNegocioException.class, () -> service.excluir(1L, "cadastro errado"));
        verify(contratoFinanceiroRepository, never()).save(any());
    }

    @Test
    void excluirContratoAtivoMarcaParcelasInativasEZeraValorDoLoteQuandoUnico() {
        ContratoFinanceiroModel contrato = contratoParaExcluir(SituacaoContrato.ATIVO);
        when(contratoFinanceiroRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(contrato));
        when(contratoFinanceiroRepository.findByImovelId(1L)).thenReturn(List.of(contrato));
        when(contratoDocumentoRepository.findByContratoId(1L)).thenReturn(List.of());
        when(despesaRepository.findByContratoFinanceiroId(1L)).thenReturn(List.of());
        when(usuarioAutenticadoService.usuarioAtual()).thenReturn(UsuarioModel.builder().id(9L).build());

        service.excluir(1L, "cadastro errado");

        assertThat(contrato.getExclusao().getAtivo()).isFalse();
        assertThat(contrato.getExclusao().getMotivoExclusao()).isEqualTo("cadastro errado");
        assertThat(contrato.getParcelas()).allMatch(p -> Boolean.FALSE.equals(p.getExclusao().getAtivo()));
        assertThat(contrato.getImovel().getCompra().getValor()).isNull();
        verify(imovelRepository).save(contrato.getImovel());
    }

    @Test
    void excluirDesvinculaDespesaDeCustoAcessorioSemExcluiLa() {
        ContratoFinanceiroModel contrato = contratoParaExcluir(SituacaoContrato.ATIVO);
        DespesaModel vistoria = DespesaModel.builder().id(50L).contratoFinanceiro(contrato).build();
        when(contratoFinanceiroRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(contrato));
        when(contratoFinanceiroRepository.findByImovelId(1L)).thenReturn(List.of());
        when(contratoDocumentoRepository.findByContratoId(1L)).thenReturn(List.of());
        when(despesaRepository.findByContratoFinanceiroId(1L)).thenReturn(List.of(vistoria));
        when(usuarioAutenticadoService.usuarioAtual()).thenReturn(UsuarioModel.builder().id(9L).build());

        service.excluir(1L, "cadastro errado");

        assertThat(vistoria.getContratoFinanceiro()).isNull();
        verify(despesaRepository).saveAll(List.of(vistoria));
    }

    // Cobre ADR-043: cascata de venda desfeita e rastreio de estorno.
    @Test
    void cancelarPorVendaDesfeitaGravaSituacaoDataMotivoEZeraEstorno() {
        ContratoFinanceiroModel contrato = contratoDeVenda();
        when(contratoFinanceiroRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(contrato));
        when(contratoFinanceiroRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        service.cancelarPorVendaDesfeita(1L, "comprador desistiu", LocalDate.of(2026, 9, 20));

        assertThat(contrato.getSituacao()).isEqualTo(SituacaoContrato.CANCELADO);
        assertThat(contrato.getDataCancelamento()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(contrato.getMotivoCancelamento()).isEqualTo("comprador desistiu");
        assertThat(contrato.getValorEstornado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void registrarEstornoRecusaContratoNaoCancelado() {
        ContratoFinanceiroModel contrato = contratoDeVenda();
        when(contratoFinanceiroRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(contrato));

        assertThrows(RegraDeNegocioException.class, () ->
                service.registrarEstorno(1L, new ContratoEstornoRequestDTO(LocalDate.now(), new BigDecimal("100"))));
        verify(contratoFinanceiroRepository, never()).save(any());
    }

    @Test
    void registrarEstornoRecusaValorMaiorQueOSaldoAindaADevolver() {
        ContratoFinanceiroModel contrato = contratoDeVenda();
        contrato.setSituacao(SituacaoContrato.CANCELADO);
        contrato.setValorEstornado(BigDecimal.ZERO);
        when(contratoFinanceiroRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(contrato));

        // Só 5.000 foi pago na parcela (ver contratoDeVenda()); pedir mais que isso é recusado.
        assertThrows(RegraDeNegocioException.class, () ->
                service.registrarEstorno(1L, new ContratoEstornoRequestDTO(LocalDate.now(), new BigDecimal("9999"))));
        verify(contratoFinanceiroRepository, never()).save(any());
    }

    @Test
    void registrarEstornoAcumulaOValorDevolvidoEGravaAData() {
        ContratoFinanceiroModel contrato = contratoDeVenda();
        contrato.setSituacao(SituacaoContrato.CANCELADO);
        contrato.setValorEstornado(new BigDecimal("1000"));
        when(contratoFinanceiroRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(contrato));
        when(contratoFinanceiroRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        service.registrarEstorno(1L, new ContratoEstornoRequestDTO(LocalDate.of(2026, 10, 1), new BigDecimal("2000")));

        assertThat(contrato.getValorEstornado()).isEqualByComparingTo("3000");
        assertThat(contrato.getDataEstorno()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    // Parcela nº 1 paga em 5.000 — o total que precisaria ser devolvido se a venda caísse.
    private ContratoFinanceiroModel contratoDeVenda() {
        ContratoFinanceiroModel contrato = ContratoFinanceiroModel.builder()
                .id(1L)
                .tipo(TipoContratoFinanceiro.PARCELAMENTO_VENDA)
                .situacao(SituacaoContrato.ATIVO)
                .valorContratado(new BigDecimal("50000"))
                .build();
        ParcelaContratoModel paga = ParcelaContratoModel.builder().contrato(contrato).numero(1)
                .dataVencimento(LocalDate.of(2026, 9, 15)).valor(new BigDecimal("5000"))
                .dataPagamento(LocalDate.of(2026, 9, 15)).valorPago(new BigDecimal("5000")).build();
        contrato.setParcelas(new java.util.ArrayList<>(List.of(paga)));
        return contrato;
    }

    private ContratoFinanceiroModel contratoParaExcluir(SituacaoContrato situacao) {
        ImovelModel imovel = ImovelModel.builder()
                .id(1L)
                .identificador("LOT-001")
                .compra(DadosCompra.builder().valor(new BigDecimal("50000")).data(LocalDate.of(2026, 8, 15)).build())
                .build();
        ContratoFinanceiroModel contrato = ContratoFinanceiroModel.builder()
                .id(1L)
                .imovel(imovel)
                .tipo(TipoContratoFinanceiro.PARCELAMENTO_COMPRA)
                .situacao(situacao)
                .valorContratado(new BigDecimal("50000"))
                .build();
        contrato.setParcelas(new java.util.ArrayList<>(List.of(
                ParcelaContratoModel.builder().contrato(contrato).numero(1)
                        .dataVencimento(LocalDate.of(2026, 9, 15)).valor(new BigDecimal("5000")).build())));
        return contrato;
    }

    private ParcelaContratoModel capturarParcela(int numero) {
        ArgumentCaptor<ContratoFinanceiroModel> captor = ArgumentCaptor.forClass(ContratoFinanceiroModel.class);
        verify(contratoFinanceiroRepository).save(captor.capture());
        return captor.getValue().getParcelas().stream()
                .filter(p -> p.getNumero() == numero)
                .findFirst()
                .orElse(null);
    }

    private void mockar(ImovelModel imovel) {
        when(imovelRepository.findByIdAndAtivoTrue(anyLong())).thenReturn(Optional.of(imovel));
        when(pessoaRepository.findByIdAndAtivoTrue(anyLong())).thenReturn(Optional.of(new PessoaModel()));
        when(contratoFinanceiroRepository.save(any(ContratoFinanceiroModel.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private ImovelModel imovel(BigDecimal valorCompra) {
        return ImovelModel.builder()
                .id(1L)
                .identificador("LOT-001")
                .compra(DadosCompra.builder()
                        .valor(valorCompra)
                        .data(LocalDate.of(2026, 8, 15))
                        .parcelada(true)
                        .build())
                .build();
    }

    // Lote de 50.000: entrada 20.000 + 6 x 5.000.
    private ContratoFinanceiroRequestDTO requisicao(ImovelModel imovel, BigDecimal entrada, BigDecimal precoAVista) {
        List<ParcelaContratoRequestDTO> parcelas = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> new ParcelaContratoRequestDTO(i, LocalDate.of(2026, 9, 15).plusMonths(i - 1),
                        new BigDecimal("5000"), null))
                .toList();

        return new ContratoFinanceiroRequestDTO(imovel.getId(), TipoContratoFinanceiro.PARCELAMENTO_COMPRA, 1L,
                new BigDecimal("50000"), parcelas, entrada, LocalDate.of(2026, 8, 15), precoAVista);
    }
}
