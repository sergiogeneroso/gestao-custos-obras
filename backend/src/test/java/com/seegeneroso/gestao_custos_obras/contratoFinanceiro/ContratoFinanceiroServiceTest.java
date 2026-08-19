package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ParcelaContratoRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.DadosCompra;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
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
    private StorageService storageService;
    @Mock
    private ContratoFinanceiroMapper contratoFinanceiroMapper;

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
                .ativo(true)
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
