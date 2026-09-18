package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaRequestDTO;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaResponseDTO;
import com.seegeneroso.gestao_custos_obras.imovel.DadosCompra;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.EtapaConstrucao;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoAnexoDespesa;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.OperacaoAuditoria;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import com.seegeneroso.gestao_custos_obras.shared.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cobre as duas regras que a despesa carrega sozinha (.agents/rules/ciclo-vida-imovel.md e
// regras-negocio-financeiras.md, ADR-035): a etapa de obra só existe na fase CONSTRUCAO, e a fase
// gravada é a que o usuário informou — herdar a fase atual do imóvel é só o padrão de quem não
// informou nada, porque lançamento retroativo precisa cair na fase em que o gasto foi incorrido.
@ExtendWith(MockitoExtension.class)
class DespesaServiceTest {

    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private ImovelRepository imovelRepository;
    @Mock
    private CategoriaDespesaRepository categoriaDespesaRepository;
    @Mock
    private PessoaRepository pessoaRepository;
    @Mock
    private ContratoFinanceiroRepository contratoFinanceiroRepository;
    @Mock
    private DespesaAnexoRepository despesaAnexoRepository;
    @Mock
    private StorageService storageService;
    @Spy
    private DespesaMapper despesaMapper = new DespesaMapper();
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private UsuarioAutenticadoService usuarioAutenticadoService;

    @InjectMocks
    private DespesaService despesaService;

    private static final LocalDate PAGAMENTO = LocalDate.of(2026, 8, 20);

    @Test
    void etapaDeObraEmDespesaDeLoteEhRecusada() {
        mockarDependencias(imovel(FaseImovel.LOTE));

        assertThatThrownBy(() -> despesaService.criar(dto(1L, null, EtapaConstrucao.FUNDACAO)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("fase Construção");
        verify(despesaRepository, never()).save(any());
    }

    // Gasto geral não tem imóvel e, portanto, não tem fase — nem etapa de obra.
    @Test
    void etapaDeObraEmGastoGeralEhRecusada() {
        mockarPessoaECategoria();

        assertThatThrownBy(() -> despesaService.criar(dto(null, null, EtapaConstrucao.ALVENARIA)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("fase Construção");
        verify(despesaRepository, never()).save(any());
    }

    @Test
    void etapaDeObraEmDespesaDeConstrucaoEhAceita() {
        mockarDependencias(imovel(FaseImovel.CONSTRUCAO));
        when(despesaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        despesaService.criar(dto(1L, null, EtapaConstrucao.COBERTURA));

        assertThat(capturarSalva().getEtapaConstrucao()).isEqualTo(EtapaConstrucao.COBERTURA);
    }

    // A fase informada vence a fase atual do imóvel: é o que faz lançamento retroativo (uma conta
    // do tempo de lote que só chegou depois da obra começar) cair no lugar certo do relatório.
    @Test
    void faseInformadaVenceAFaseAtualDoImovel() {
        mockarDependencias(imovel(FaseImovel.CONSTRUCAO));
        when(despesaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        despesaService.criar(dto(1L, FaseImovel.LOTE, null));

        assertThat(capturarSalva().getFaseImovel()).isEqualTo(FaseImovel.LOTE);
    }

    @Test
    void semFaseInformadaHerdaAFaseAtualDoImovel() {
        mockarDependencias(imovel(FaseImovel.CONSTRUCAO));
        when(despesaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        despesaService.criar(dto(1L, null, null));

        assertThat(capturarSalva().getFaseImovel()).isEqualTo(FaseImovel.CONSTRUCAO);
    }

    // Etapa informada junto de uma fase retroativa é recusada pela fase do DTO, não pela do imóvel.
    @Test
    void etapaDeObraComFaseRetroativaDeLoteEhRecusada() {
        mockarDependencias(imovel(FaseImovel.CONSTRUCAO));

        assertThatThrownBy(() -> despesaService.criar(dto(1L, FaseImovel.LOTE, EtapaConstrucao.FUNDACAO)))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(despesaRepository, never()).save(any());
    }

    @Test
    void gastoGeralNaoTemImovelNemFase() {
        mockarPessoaECategoria();
        when(despesaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        despesaService.criar(dto(null, null, null));

        DespesaModel salva = capturarSalva();
        assertThat(salva.getImovel()).isNull();
        assertThat(salva.getFaseImovel()).isNull();
    }

    @Test
    void buscaContaAnexosDaPaginaESomaZeroParaDespesaSemAnexo() {
        DespesaModel comAnexos = DespesaModel.builder().id(10L).build();
        DespesaModel semAnexo = DespesaModel.builder().id(11L).build();
        when(despesaRepository.buscar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(comAnexos, semAnexo)));
        // Um id por anexo: a despesa 10 tem dois, a 11 não aparece porque não tem nenhum.
        when(despesaAnexoRepository.listarDespesaIdPorAnexo(List.of(10L, 11L)))
                .thenReturn(List.of(10L, 10L));

        List<DespesaResponseDTO> conteudo = despesaService.buscar("", "TODAS", 0, 20).conteudo();

        assertThat(conteudo).extracting(DespesaResponseDTO::quantidadeAnexos).containsExactly(2, 0);
    }

    @Test
    void buscarPorIdNaoInformaQuantidadeNemComprovante() {
        when(despesaRepository.findByIdAndAtivoTrue(10L))
                .thenReturn(Optional.of(DespesaModel.builder().id(10L).build()));

        // Nulo é "não calculado": a tela não pode ler isso como despesa sem comprovante.
        DespesaResponseDTO resultado = despesaService.buscarPorId(10L);
        assertThat(resultado.quantidadeAnexos()).isNull();
        assertThat(resultado.temComprovante()).isNull();
    }

    // ADR-023: só o tipo COMPROVANTE prova o pagamento — RECIBO e os demais tipos não contam.
    @Test
    void buscaMarcaTemComprovanteSoParaDespesaComAnexoDoTipoComprovante() {
        DespesaModel comComprovante = DespesaModel.builder().id(10L).build();
        DespesaModel semComprovante = DespesaModel.builder().id(11L).build();
        when(despesaRepository.buscar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(comComprovante, semComprovante)));
        when(despesaAnexoRepository.listarDespesaIdPorAnexo(List.of(10L, 11L))).thenReturn(List.of());
        when(despesaAnexoRepository.listarDespesaIdPorAnexoDoTipo(List.of(10L, 11L), TipoAnexoDespesa.COMPROVANTE))
                .thenReturn(List.of(10L));

        List<DespesaResponseDTO> conteudo = despesaService.buscar("", "TODAS", 0, 20).conteudo();

        assertThat(conteudo).extracting(DespesaResponseDTO::temComprovante).containsExactly(true, false);
    }

    // Bug corrigido (issue 04, decisão 7 da spec): buscarContratoOpcional usava findById sem
    // filtro de ativo, então um contrato excluído era encontrado normalmente em vez de recusado.
    @Test
    void contratoExcluidoEhTratadoComoNaoEncontrado() {
        mockarPessoaECategoria();
        when(contratoFinanceiroRepository.findByIdAndAtivoTrue(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despesaService.criar(dto(null, null, null, 5L)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Contrato financeiro não encontrado");
        verify(despesaRepository, never()).save(any());
    }

    // contratos-financeiros.md: custo acessório do financiamento (vistoria, avaliação, tarifa...) é
    // despesa comum vinculada por FK opcional a um contrato válido — grava o vínculo sem tocar em
    // nada do contrato, porque prestação nunca é despesa e despesa nunca é prestação.
    @Test
    void despesaDeCustoAcessorioVinculadaAContratoAtivoGravaOVinculoSemAlterarOContrato() {
        mockarDependencias(imovel(FaseImovel.CONSTRUCAO));
        ContratoFinanceiroModel contrato = ContratoFinanceiroModel.builder()
                .id(5L).situacao(SituacaoContrato.ATIVO)
                .valorContratado(new BigDecimal("200000.00")).parcelas(new java.util.ArrayList<>()).build();
        when(contratoFinanceiroRepository.findByIdAndAtivoTrue(5L)).thenReturn(Optional.of(contrato));
        when(despesaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        despesaService.criar(dto(1L, null, null, 5L));

        assertThat(capturarSalva().getContratoFinanceiro()).isSameAs(contrato);
        // O valor da despesa (1500.00) não altera nada do contrato — prestação e despesa são coisas
        // separadas (contratos-financeiros.md).
        assertThat(contrato.getValorContratado()).isEqualByComparingTo("200000.00");
        assertThat(contrato.getParcelas()).isEmpty();
    }

    // atualizar aplica a mesma regra de criar: etapa de obra só existe na fase Construção.
    @Test
    void atualizarAplicaAMesmaRegraDeEtapaDeConstrucaoQueCriar() {
        when(despesaRepository.findByIdAndAtivoTrue(10L))
                .thenReturn(Optional.of(DespesaModel.builder().id(10L).imovel(imovel(FaseImovel.LOTE)).build()));
        mockarDependencias(imovel(FaseImovel.LOTE));

        assertThatThrownBy(() -> despesaService.atualizar(10L, dto(1L, null, EtapaConstrucao.FUNDACAO)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("fase Construção");
        verify(despesaRepository, never()).save(any());
    }

    // Cobre .agents/rules/auditoria.md: excluir grava o estado anterior (ativo=true) antes da
    // exclusão lógica mudar o campo para false.
    @Test
    void excluirAplicaExclusaoLogicaComMotivo() {
        DespesaModel despesa = DespesaModel.builder().id(10L).build();
        when(despesaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(despesa));
        when(despesaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        despesaService.excluir(10L, "Lançamento duplicado");

        assertThat(despesa.getExclusao().getAtivo()).isFalse();
        assertThat(despesa.getExclusao().getMotivoExclusao()).isEqualTo("Lançamento duplicado");

        ArgumentCaptor<DespesaResponseDTO> anteriorCaptor = ArgumentCaptor.forClass(DespesaResponseDTO.class);
        ArgumentCaptor<DespesaResponseDTO> novoCaptor = ArgumentCaptor.forClass(DespesaResponseDTO.class);
        verify(auditoriaService).registrar(eq("Despesa"), eq(10L), eq(OperacaoAuditoria.EXCLUSAO),
                anteriorCaptor.capture(), novoCaptor.capture());
        assertThat(anteriorCaptor.getValue().ativo()).isTrue();
        assertThat(novoCaptor.getValue().ativo()).isFalse();
    }

    // Cobre .agents/rules/auditoria.md: criar audita CRIACAO com estadoAnterior nulo.
    @Test
    void criarAuditaCriacaoComEstadoAnteriorNulo() {
        mockarDependencias(imovel(FaseImovel.LOTE));
        when(despesaRepository.save(any())).thenAnswer(chamada -> {
            DespesaModel salva = chamada.getArgument(0);
            salva.setId(77L);
            return salva;
        });

        despesaService.criar(dto(1L, null, null));

        ArgumentCaptor<DespesaResponseDTO> novoCaptor = ArgumentCaptor.forClass(DespesaResponseDTO.class);
        verify(auditoriaService).registrar(eq("Despesa"), eq(77L), eq(OperacaoAuditoria.CRIACAO), isNull(), novoCaptor.capture());
        assertThat(novoCaptor.getValue().valor()).isEqualByComparingTo("1500.00");
    }

    // Cobre .agents/rules/auditoria.md: atualizar captura o estadoAnterior antes de aplicar os
    // setters — provado comparando o valor, que muda de 1000 (estado original) para 1500 (do dto).
    @Test
    void atualizarAuditaComEstadoAnteriorCapturadoAntesDaMutacao() {
        DespesaModel existente = DespesaModel.builder().id(10L).imovel(imovel(FaseImovel.LOTE))
                .valor(new BigDecimal("1000.00")).dataPagamento(PAGAMENTO).build();
        when(despesaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(existente));
        mockarDependencias(imovel(FaseImovel.LOTE));
        when(despesaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        despesaService.atualizar(10L, dto(1L, null, null));

        ArgumentCaptor<DespesaResponseDTO> anteriorCaptor = ArgumentCaptor.forClass(DespesaResponseDTO.class);
        ArgumentCaptor<DespesaResponseDTO> novoCaptor = ArgumentCaptor.forClass(DespesaResponseDTO.class);
        verify(auditoriaService).registrar(eq("Despesa"), eq(10L), eq(OperacaoAuditoria.EDICAO),
                anteriorCaptor.capture(), novoCaptor.capture());

        assertThat(anteriorCaptor.getValue().valor()).isEqualByComparingTo("1000.00");
        assertThat(novoCaptor.getValue().valor()).isEqualByComparingTo("1500.00");
    }

    // Sub-recurso (anexo de despesa) não audita — mesma fronteira de fotos/documentos.
    @Test
    void adicionarAnexoNaoGeraEventoDeAuditoria() {
        DespesaModel despesa = DespesaModel.builder().id(10L).build();
        when(despesaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(despesa));
        org.springframework.web.multipart.MultipartFile arquivo = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(storageService.salvar(any(), any())).thenReturn("recibo.pdf");
        when(despesaAnexoRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        despesaService.adicionarAnexo(10L, arquivo, TipoAnexoDespesa.RECIBO);

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void deletarAnexoRecusaAnexoDeOutraDespesa() {
        DespesaAnexoModel anexoDeOutraDespesa = DespesaAnexoModel.builder()
                .id(99L)
                .despesa(DespesaModel.builder().id(20L).build())
                .build();
        when(despesaAnexoRepository.findById(99L)).thenReturn(Optional.of(anexoDeOutraDespesa));

        assertThatThrownBy(() -> despesaService.deletarAnexo(10L, 99L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não pertence à despesa");
        verify(despesaAnexoRepository, never()).delete(any());
    }

    // ADR-040: Pessoa com exclusao.ativo = false não pode ser vinculada a uma nova despesa.
    @Test
    void pagadorInativoNaoPodeSerVinculado() {
        when(categoriaDespesaRepository.findById(2L))
                .thenReturn(Optional.of(CategoriaDespesaModel.builder().id(2L).nome("Material").build()));
        when(pessoaRepository.findByIdAndAtivoTrue(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despesaService.criar(dto(null, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Pagador não encontrado");
        verify(despesaRepository, never()).save(any());
    }

    // ADR-040: Imovel com exclusao.ativo = false não aceita nova despesa.
    @Test
    void imovelInativoNaoAceitaDespesa() {
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despesaService.criar(dto(1L, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Imóvel não encontrado");
        verify(despesaRepository, never()).save(any());
    }

    // Imóvel vendido continua aceitando despesa (ciclo-vida-imovel.md): obra em andamento,
    // corretagem e acertos chegam depois da venda e são custo real. DespesaService não filtra por
    // situação — este teste prova que criar() não recusa o caso.
    @Test
    void imovelVendidoAceitaDespesa() {
        ImovelModel imovel = imovel(FaseImovel.CONSTRUCAO);
        imovel.setSituacao(SituacaoImovel.VENDIDO);
        mockarDependencias(imovel);
        when(despesaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        despesaService.criar(dto(1L, FaseImovel.CONSTRUCAO, null));

        verify(despesaRepository).save(any());
    }

    private DespesaModel capturarSalva() {
        ArgumentCaptor<DespesaModel> captor = ArgumentCaptor.forClass(DespesaModel.class);
        verify(despesaRepository).save(captor.capture());
        return captor.getValue();
    }

    private void mockarDependencias(ImovelModel imovel) {
        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        mockarPessoaECategoria();
    }

    private void mockarPessoaECategoria() {
        when(categoriaDespesaRepository.findById(2L))
                .thenReturn(Optional.of(CategoriaDespesaModel.builder().id(2L).nome("Material").build()));
        when(pessoaRepository.findByIdAndAtivoTrue(3L))
                .thenReturn(Optional.of(PessoaModel.builder().id(3L).nome("Pagador").build()));
    }

    private ImovelModel imovel(FaseImovel fase) {
        return ImovelModel.builder()
                .id(1L)
                .identificador("LOTE-01")
                .fase(fase)
                .situacao(SituacaoImovel.ADQUIRIDO)
                .compra(DadosCompra.builder().data(LocalDate.of(2026, 1, 10)).build())
                .build();
    }

    private DespesaRequestDTO dto(Long imovelId, FaseImovel fase, EtapaConstrucao etapa) {
        return dto(imovelId, fase, etapa, null);
    }

    private DespesaRequestDTO dto(Long imovelId, FaseImovel fase, EtapaConstrucao etapa, Long contratoFinanceiroId) {
        return new DespesaRequestDTO(imovelId, 2L, 3L, null, contratoFinanceiroId, fase, etapa,
                new BigDecimal("1500.00"), PAGAMENTO, "Compra de material", null);
    }
}
