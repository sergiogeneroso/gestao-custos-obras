package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroService;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ParcelaContratoModel;
import com.seegeneroso.gestao_custos_obras.imovel.dto.DadosCasaDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.DadosConstrucaoDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFaseRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelResponseDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelSituacaoRequestDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.OperacaoAuditoria;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import com.seegeneroso.gestao_custos_obras.shared.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    private ContratoFinanceiroService contratoFinanceiroService;
    @Mock
    private StorageService storageService;
    @Spy
    private ImovelMapper imovelMapper = new ImovelMapper();
    @Mock
    private AuditoriaService auditoriaService;

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

    // Pular etapa não é "retroceder", mas a mesma regra de sequência estrita recusa os dois
    // (ADR-020): só a próxima fase da ordem é destino válido.
    @Test
    void transicaoQuePulaFaseEhRecusada() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));

        ImovelFaseRequestDTO dto = new ImovelFaseRequestDTO(FaseImovel.CASA, COMPRA.plusMonths(2), null, null);

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

    // fase e situação são eixos independentes (ADR-020): vender não mexe na fase.
    @Test
    void venderGravaValorDataCompradorSemAlterarFase() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        PessoaModel comprador = PessoaModel.builder().id(5L).nome("Comprador").build();
        when(pessoaRepository.findByIdAndAtivoTrue(5L)).thenReturn(Optional.of(comprador));

        ImovelSituacaoRequestDTO dto = new ImovelSituacaoRequestDTO(
                SituacaoImovel.VENDIDO, new BigDecimal("250000"), LocalDate.of(2026, 6, 1), 5L, null, null);
        imovelService.alterarSituacao(1L, dto);

        assertThat(imovel.getVenda().getValor()).isEqualByComparingTo("250000");
        assertThat(imovel.getVenda().getData()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(imovel.getVenda().getComprador()).isEqualTo(comprador);
        assertThat(imovel.getFase()).isEqualTo(FaseImovel.LOTE);
    }

    // Eixo inverso da anterior: avançar a fase de um imóvel já vendido não mexe na situação
    // comercial — a obra segue depois da venda (ciclo-vida-imovel.md).
    @Test
    void avancarFaseDeImovelVendidoNaoAlteraSituacao() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(contratoFinanceiroRepository.findByImovelId(anyLong())).thenReturn(List.of());

        imovelService.avancarFase(1L, new ImovelFaseRequestDTO(FaseImovel.CONSTRUCAO, COMPRA.plusMonths(2), null, null));

        assertThat(imovel.getFase()).isEqualTo(FaseImovel.CONSTRUCAO);
        assertThat(imovel.getSituacao()).isEqualTo(SituacaoImovel.VENDIDO);
    }

    // Colocar à venda é quando o valor pretendido é decidido (ADR-033).
    @Test
    void colocarAVendaGravaValorPretendido() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        ImovelSituacaoRequestDTO dto = new ImovelSituacaoRequestDTO(
                SituacaoImovel.A_VENDA, null, null, null, new BigDecimal("280000"), null);
        imovelService.alterarSituacao(1L, dto);

        assertThat(imovel.getVenda().getValorPretendido()).isEqualByComparingTo("280000");
    }

    // Cobre ADR-043: valorPretendido não é dado da venda desfeita, então desfazer venda não o toca.
    @Test
    void desfazerVendaPreservaValorPretendido() {
        ImovelModel imovel = imovel(FaseImovel.CASA);
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        imovel.getVenda().setValorPretendido(new BigDecimal("280000"));
        imovel.getVenda().setValor(new BigDecimal("300000"));
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(contratoFinanceiroRepository.findByImovelId(1L)).thenReturn(List.of());

        imovelService.alterarSituacao(1L, new ImovelSituacaoRequestDTO(
                SituacaoImovel.A_VENDA, null, null, null, null, "comprador desistiu"));

        assertThat(imovel.getVenda().getValorPretendido()).isEqualByComparingTo("280000");
    }

    // Cobre ADR-020: fase e situação só mudam pelo PATCH dedicado, nunca pelo PUT de cadastro.
    @Test
    void putNaoMudaFaseNemSituacao() {
        ImovelModel imovel = imovel(FaseImovel.CONSTRUCAO);
        imovel.setSituacao(SituacaoImovel.A_VENDA);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        imovelService.atualizar(1L, dtoCom(null, null));

        assertThat(imovel.getFase()).isEqualTo(FaseImovel.CONSTRUCAO);
        assertThat(imovel.getSituacao()).isEqualTo(SituacaoImovel.A_VENDA);
    }

    // Terceira combinação de validarOrdemDatas: sem início de obra registrado, a conclusão não
    // pode ficar antes da própria compra (ciclo-vida-imovel.md).
    @Test
    void conclusaoAntesDaCompraSemInicioDeObraEhRecusada() {
        ImovelModel imovel = imovel(FaseImovel.CASA);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));

        ImovelRequestDTO dto = dtoCom(null, casaCom(COMPRA.minusDays(10)));

        assertThatThrownBy(() -> imovelService.atualizar(1L, dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("conclusão da obra não pode ser anterior à data da compra");
        verify(imovelRepository, never()).save(any());
    }

    // Aviso (não bloqueio) quando ainda há PARCELAMENTO_COMPRA ativo ao iniciar a construção —
    // banco costuma exigir o terreno quitado para financiar a obra (ciclo-vida-imovel.md).
    @Test
    void avisoDeParcelamentoCompraAtivoAoAvancarParaConstrucao() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        ContratoFinanceiroModel contrato = ContratoFinanceiroModel.builder()
                .tipo(TipoContratoFinanceiro.PARCELAMENTO_COMPRA).situacao(SituacaoContrato.ATIVO).build();
        when(contratoFinanceiroRepository.findByImovelId(1L)).thenReturn(List.of(contrato));

        ImovelResponseDTO resultado = imovelService.avancarFase(1L,
                new ImovelFaseRequestDTO(FaseImovel.CONSTRUCAO, COMPRA.plusMonths(2), null, null));

        assertThat(resultado.aviso()).contains("PARCELAMENTO_COMPRA");
    }

    // Cobre ADR-043: desfazer venda nunca é bloqueado pelo estado do contrato, e cascateia nele.
    @Test
    void desfazerVendaSemMotivoEhRecusado() {
        ImovelModel imovel = imovel(FaseImovel.CASA);
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));

        ImovelSituacaoRequestDTO dto = new ImovelSituacaoRequestDTO(SituacaoImovel.A_VENDA, null, null, null, null, null);

        assertThatThrownBy(() -> imovelService.alterarSituacao(1L, dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Motivo");
        verify(imovelRepository, never()).save(any());
    }

    @Test
    void desfazerVendaLimpaCamposDeVenda() {
        ImovelModel imovel = imovel(FaseImovel.CASA);
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        imovel.getVenda().setValor(new BigDecimal("300000"));
        imovel.getVenda().setData(LocalDate.of(2026, 6, 1));
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(contratoFinanceiroRepository.findByImovelId(1L)).thenReturn(List.of());

        ImovelSituacaoRequestDTO dto = new ImovelSituacaoRequestDTO(
                SituacaoImovel.A_VENDA, null, null, null, null, "comprador desistiu");
        imovelService.alterarSituacao(1L, dto);

        assertThat(imovel.getVenda().getValor()).isNull();
        assertThat(imovel.getVenda().getData()).isNull();
        assertThat(imovel.getVenda().getComprador()).isNull();
    }

    @Test
    void desfazerVendaExcluiContratoDeVendaSemParcelaPaga() {
        ImovelModel imovel = imovel(FaseImovel.CASA);
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        ContratoFinanceiroModel contrato = ContratoFinanceiroModel.builder()
                .id(9L).tipo(TipoContratoFinanceiro.PARCELAMENTO_VENDA).situacao(SituacaoContrato.ATIVO)
                .parcelas(new java.util.ArrayList<>()).build();
        when(contratoFinanceiroRepository.findByImovelId(1L)).thenReturn(List.of(contrato));

        imovelService.alterarSituacao(1L, new ImovelSituacaoRequestDTO(
                SituacaoImovel.A_VENDA, null, null, null, null, "caiu"));

        verify(contratoFinanceiroService).excluir(9L, "caiu");
        verify(contratoFinanceiroService, never()).cancelarPorVendaDesfeita(any(), any(), any());
    }

    @Test
    void desfazerVendaCancelaContratoDeVendaComParcelaPaga() {
        ImovelModel imovel = imovel(FaseImovel.CASA);
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        ContratoFinanceiroModel contrato = ContratoFinanceiroModel.builder()
                .id(9L).tipo(TipoContratoFinanceiro.PARCELAMENTO_VENDA).situacao(SituacaoContrato.ATIVO).build();
        ParcelaContratoModel paga = ParcelaContratoModel.builder().contrato(contrato).numero(1)
                .dataVencimento(LocalDate.of(2026, 6, 1)).valor(new BigDecimal("5000"))
                .dataPagamento(LocalDate.of(2026, 6, 1)).valorPago(new BigDecimal("5000")).build();
        contrato.setParcelas(new java.util.ArrayList<>(List.of(paga)));
        when(contratoFinanceiroRepository.findByImovelId(1L)).thenReturn(List.of(contrato));

        imovelService.alterarSituacao(1L, new ImovelSituacaoRequestDTO(
                SituacaoImovel.A_VENDA, null, null, null, null, "caiu"));

        verify(contratoFinanceiroService).cancelarPorVendaDesfeita(eq(9L), eq("caiu"), any());
        verify(contratoFinanceiroService, never()).excluir(any(), any());
    }

    // Cobre .agents/rules/auditoria.md: atualizar, avancarFase e alterarSituacao gravam o evento
    // certo com o estadoAnterior capturado antes da mutação em memória.
    @Test
    void atualizarAuditaComEstadoAnteriorCapturadoAntesDaMutacao() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(imovelRepository.existsByIdentificadorIgnoreCase("LOTE-02")).thenReturn(false);

        // Identificador muda de LOTE-01 (estado original de imovel(...)) para LOTE-02.
        ImovelRequestDTO dto = new ImovelRequestDTO("LOTE-02", null, null, null, null, null, null, null,
                null, null, null, new BigDecimal("100000"), COMPRA, null, false, null, null);

        imovelService.atualizar(1L, dto);

        ArgumentCaptor<ImovelResponseDTO> anteriorCaptor = ArgumentCaptor.forClass(ImovelResponseDTO.class);
        ArgumentCaptor<ImovelResponseDTO> novoCaptor = ArgumentCaptor.forClass(ImovelResponseDTO.class);
        verify(auditoriaService).registrar(eq("Imovel"), eq(1L), eq(OperacaoAuditoria.EDICAO),
                anteriorCaptor.capture(), novoCaptor.capture());

        assertThat(anteriorCaptor.getValue().identificador()).isEqualTo("LOTE-01");
        assertThat(novoCaptor.getValue().identificador()).isEqualTo("LOTE-02");
    }

    @Test
    void avancarFaseAuditaComEstadoAnteriorCapturadoAntesDaMutacao() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(contratoFinanceiroRepository.findByImovelId(anyLong())).thenReturn(List.of());

        imovelService.avancarFase(1L, new ImovelFaseRequestDTO(FaseImovel.CONSTRUCAO, COMPRA.plusMonths(2), null, null));

        ArgumentCaptor<ImovelResponseDTO> anteriorCaptor = ArgumentCaptor.forClass(ImovelResponseDTO.class);
        ArgumentCaptor<ImovelResponseDTO> novoCaptor = ArgumentCaptor.forClass(ImovelResponseDTO.class);
        verify(auditoriaService).registrar(eq("Imovel"), eq(1L), eq(OperacaoAuditoria.EDICAO),
                anteriorCaptor.capture(), novoCaptor.capture());

        assertThat(anteriorCaptor.getValue().fase()).isEqualTo(FaseImovel.LOTE);
        assertThat(novoCaptor.getValue().fase()).isEqualTo(FaseImovel.CONSTRUCAO);
    }

    @Test
    void alterarSituacaoAuditaComEstadoAnteriorCapturadoAntesDaMutacao() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        imovelService.alterarSituacao(1L, new ImovelSituacaoRequestDTO(
                SituacaoImovel.A_VENDA, null, null, null, new BigDecimal("120000"), null));

        ArgumentCaptor<ImovelResponseDTO> anteriorCaptor = ArgumentCaptor.forClass(ImovelResponseDTO.class);
        ArgumentCaptor<ImovelResponseDTO> novoCaptor = ArgumentCaptor.forClass(ImovelResponseDTO.class);
        verify(auditoriaService).registrar(eq("Imovel"), eq(1L), eq(OperacaoAuditoria.EDICAO),
                anteriorCaptor.capture(), novoCaptor.capture());

        assertThat(anteriorCaptor.getValue().situacao()).isEqualTo(SituacaoImovel.ADQUIRIDO);
        assertThat(novoCaptor.getValue().situacao()).isEqualTo(SituacaoImovel.A_VENDA);
    }

    // Sub-recurso (foto do imóvel) não audita — mesma fronteira de documentos/anexos.
    @Test
    void adicionarFotoNaoGeraEventoDeAuditoria() {
        ImovelModel imovel = imovel(FaseImovel.LOTE);
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(imovelFotoRepository.findByImovelIdAndPrincipalTrue(1L)).thenReturn(Optional.empty());
        org.springframework.web.multipart.MultipartFile arquivo = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(storageService.salvar(any(), any())).thenReturn("foto.jpg");
        when(imovelFotoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        imovelService.adicionarFoto(1L, arquivo, "fachada");

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void deletarFotoRecusaFotoDeOutroImovel() {
        ImovelFotoModel foto = ImovelFotoModel.builder().id(7L).imovel(imovel(FaseImovel.LOTE))
                .url("imoveis/1/foto.jpg").build();
        when(imovelFotoRepository.findById(7L)).thenReturn(Optional.of(foto));

        assertThatThrownBy(() -> imovelService.deletarFoto(2L, 7L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não pertence");
        verify(imovelFotoRepository, never()).delete(any());
    }

    @Test
    void deletarDocumentoRecusaDocumentoDeOutroImovel() {
        ImovelDocumentoModel documento = ImovelDocumentoModel.builder().id(8L).imovel(imovel(FaseImovel.LOTE))
                .url("imoveis/1/documentos/doc.pdf").build();
        when(imovelDocumentoRepository.findById(8L)).thenReturn(Optional.of(documento));

        assertThatThrownBy(() -> imovelService.deletarDocumento(2L, 8L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não pertence");
        verify(imovelDocumentoRepository, never()).delete(any());
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
