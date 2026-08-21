package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.imovel.dto.DadosCasaDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.DadosConstrucaoDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFaseRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelRequestDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import com.seegeneroso.gestao_custos_obras.shared.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cobre as regras do ciclo de vida que o PUT e a transição de fase não podem quebrar
// (.agents/rules/ciclo-vida-imovel.md): ordem das datas nos dois caminhos e identificador único
// sem depender do case. O mapper entra como @Spy porque a validação roda sobre o estado já
// aplicado por ele — com mapper mockado o teste não exercitaria nada.
@ExtendWith(MockitoExtension.class)
class ImovelServiceTest {

    @Mock
    private ImovelRepository imovelRepository;
    @Mock
    private ImovelFotoRepository imovelFotoRepository;
    @Mock
    private ImovelDocumentoRepository imovelDocumentoRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private ContratoFinanceiroRepository contratoFinanceiroRepository;
    @Mock
    private StorageService storageService;
    @Spy
    private ImovelMapper imovelMapper = new ImovelMapper();

    @InjectMocks
    private ImovelService imovelService;

    private static final LocalDate COMPRA = LocalDate.of(2026, 1, 10);

    @Test
    void putComConclusaoDaObraAntesDoInicioEhRecusado() {
        ImovelModel imovel = imovel(FaseImovel.CASA);
        imovel.getConstrucao().setDataInicio(LocalDate.of(2026, 3, 1));
        imovel.getCasa().setDataConclusaoObra(LocalDate.of(2026, 9, 1));
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));

        ImovelRequestDTO dto = dtoCom(
                construcaoCom(LocalDate.of(2026, 3, 1)),
                casaCom(LocalDate.of(2026, 2, 1)));

        assertThatThrownBy(() -> imovelService.atualizar(1L, dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("conclusão da obra");
        verify(imovelRepository, never()).save(any());
    }

    @Test
    void putComInicioDaObraAntesDaCompraEhRecusado() {
        ImovelModel imovel = imovel(FaseImovel.CONSTRUCAO);
        imovel.getConstrucao().setDataInicio(LocalDate.of(2026, 3, 1));
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));

        ImovelRequestDTO dto = dtoCom(construcaoCom(COMPRA.minusDays(30)), null);

        assertThatThrownBy(() -> imovelService.atualizar(1L, dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("início da construção");
        verify(imovelRepository, never()).save(any());
    }

    // Lote não tem data de obra nenhuma: o validador ignora nulo em vez de recusar, senão toda
    // edição de imóvel na fase inicial quebraria.
    @Test
    void putDeLoteSemDatasDeObraPassa() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        imovelService.atualizar(1L, dtoCom(null, null));

        verify(imovelRepository).save(any());
    }

    @Test
    void transicaoComDataAnteriorAFaseAnteriorEhRecusada() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));

        ImovelFaseRequestDTO dto = new ImovelFaseRequestDTO(
                FaseImovel.CONSTRUCAO, COMPRA.minusDays(5), null, null);

        assertThatThrownBy(() -> imovelService.avancarFase(1L, dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("início da construção");
        verify(imovelRepository, never()).save(any());
    }

    @Test
    void identificadorDuplicadoSoNoCaseEhRecusadoNaCriacao() {
        when(imovelRepository.existsByIdentificadorIgnoreCase("lote-01")).thenReturn(true);

        ImovelRequestDTO dto = new ImovelRequestDTO("lote-01", null, null, null, null, null, null,
                null, null, null, null, new BigDecimal("100000"), COMPRA, null, false, null, null);

        assertThatThrownBy(() -> imovelService.criar(dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("lote-01");
        verify(imovelRepository, never()).save(any());
    }

    @Test
    void identificadorDuplicadoSoNoCaseEhRecusadoNaEdicao() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.existsByIdentificadorIgnoreCase("OUTRO-LOTE")).thenReturn(true);

        ImovelRequestDTO dto = new ImovelRequestDTO("OUTRO-LOTE", null, null, null, null, null, null,
                null, null, null, null, new BigDecimal("100000"), COMPRA, null, false, null, null);

        assertThatThrownBy(() -> imovelService.atualizar(1L, dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("OUTRO-LOTE");
    }

    // A fase só avança, nunca retrocede nem pula (ADR-020).
    @Test
    void faseNaoRetrocede() {
        ImovelModel imovel = imovel(FaseImovel.CASA);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));

        ImovelFaseRequestDTO dto = new ImovelFaseRequestDTO(FaseImovel.LOTE, COMPRA, null, null);

        assertThatThrownBy(() -> imovelService.avancarFase(1L, dto))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(imovelRepository, never()).save(any());
    }

    @Test
    void avancoDeFaseGravaADataInformada() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(contratoFinanceiroRepository.findByImovelId(anyLong())).thenReturn(java.util.List.of());

        LocalDate inicioObra = COMPRA.plusMonths(2);
        imovelService.avancarFase(1L, new ImovelFaseRequestDTO(FaseImovel.CONSTRUCAO, inicioObra, null, null));

        assertThat(imovel.getFase()).isEqualTo(FaseImovel.CONSTRUCAO);
        assertThat(imovel.getConstrucao().getDataInicio()).isEqualTo(inicioObra);
    }

    private ImovelModel imovel(FaseImovel fase) {
        return ImovelModel.builder()
                .id(1L)
                .identificador("LOTE-01")
                .fase(fase)
                .situacao(SituacaoImovel.ADQUIRIDO)
                .lote(new DadosLote())
                .construcao(new DadosConstrucao())
                .casa(new DadosCasa())
                .compra(DadosCompra.builder().valor(new BigDecimal("100000")).data(COMPRA).build())
                .venda(new DadosVenda())
                .ativo(true)
                .build();
    }

    private ImovelRequestDTO dtoCom(DadosConstrucaoDTO construcao, DadosCasaDTO casa) {
        return new ImovelRequestDTO("LOTE-01", null, null, null, null, null, null, null,
                null, construcao, casa, new BigDecimal("100000"), COMPRA, null, false, null, null);
    }

    private DadosConstrucaoDTO construcaoCom(LocalDate dataInicio) {
        return new DadosConstrucaoDTO(null, dataInicio, null, null, null, null, null, null, null, null, null);
    }

    private DadosCasaDTO casaCom(LocalDate dataConclusaoObra) {
        return new DadosCasaDTO(dataConclusaoObra, null, null, null, null, null, null, null);
    }
}
