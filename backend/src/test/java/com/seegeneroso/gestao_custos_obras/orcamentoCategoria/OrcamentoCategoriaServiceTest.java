package com.seegeneroso.gestao_custos_obras.orcamentoCategoria;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaRepository;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto.OrcamentoCategoriaRequestDTO;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto.OrcamentoCategoriaResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.OperacaoAuditoria;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

// Bug corrigido (issue 04, decisão 7 da spec): a checagem de duplicidade não filtrava por ativo,
// então um orçamento excluído bloqueava indefinidamente um novo lançamento para a mesma categoria.
@ExtendWith(MockitoExtension.class)
class OrcamentoCategoriaServiceTest {

    @Mock
    private OrcamentoCategoriaRepository orcamentoCategoriaRepository;
    @Mock
    private ImovelRepository imovelRepository;
    @Mock
    private CategoriaDespesaRepository categoriaDespesaRepository;
    @Mock
    private DespesaRepository despesaRepository;
    @Spy
    private OrcamentoCategoriaMapper orcamentoCategoriaMapper = new OrcamentoCategoriaMapper();
    @Mock
    private UsuarioAutenticadoService usuarioAutenticadoService;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private OrcamentoCategoriaService orcamentoCategoriaService;

    @Test
    void duplicidadeNaCriacaoEhRecusadaQuandoJaExisteOrcamentoAtivo() {
        mockarImovelECategoria();
        when(orcamentoCategoriaRepository.existsByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> orcamentoCategoriaService.criar(dto()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe um orçamento cadastrado");
        verify(orcamentoCategoriaRepository, never()).save(any());
    }

    @Test
    void orcamentoExcluidoNaoBloqueiaCriarOutroParaMesmaCategoria() {
        mockarImovelECategoria();
        when(orcamentoCategoriaRepository.existsByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L)).thenReturn(false);
        when(orcamentoCategoriaRepository.save(any())).thenAnswer(chamada -> {
            OrcamentoCategoriaModel salvo = chamada.getArgument(0);
            salvo.setId(50L);
            return salvo;
        });
        when(despesaRepository.findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L)).thenReturn(List.of());

        OrcamentoCategoriaResponseDTO resultado = orcamentoCategoriaService.criar(dto());

        assertThat(resultado.id()).isEqualTo(50L);
    }

    @Test
    void duplicidadeNaEdicaoEhRecusadaQuandoOutroOrcamentoAtivoUsaAMesmaCategoria() {
        OrcamentoCategoriaModel existente = orcamento(10L);
        when(orcamentoCategoriaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(existente));
        when(despesaRepository.findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L)).thenReturn(List.of());
        mockarImovelECategoria();
        // Outro orçamento (id 99, diferente do que está sendo editado) já usa a mesma categoria.
        when(orcamentoCategoriaRepository.findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L))
                .thenReturn(Optional.of(orcamento(99L)));

        assertThatThrownBy(() -> orcamentoCategoriaService.atualizar(10L, dto()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe outro orçamento cadastrado");
        verify(orcamentoCategoriaRepository, never()).save(any());
    }

    // totalGasto é a soma das despesas ativas do imóvel na categoria, nunca recalculado com outra
    // conta no teste — valores redondos e distintos deixam a soma óbvia.
    @Test
    void totalGastoSomaAsDespesasAtivasDoImovelNaCategoria() {
        OrcamentoCategoriaModel orcamento = orcamento(10L);
        when(orcamentoCategoriaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(orcamento));
        when(despesaRepository.findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L)).thenReturn(List.of(
                DespesaModel.builder().valor(new BigDecimal("2000.00")).build(),
                DespesaModel.builder().valor(new BigDecimal("500.00")).build()
        ));

        BigDecimal totalGasto = orcamentoCategoriaService.buscarPorId(10L).totalGasto();

        // 2000 + 500
        assertThat(totalGasto).isEqualByComparingTo("2500.00");
    }

    // Cobre .agents/rules/auditoria.md: criar audita CRIACAO com estadoAnterior nulo.
    @Test
    void criarAuditaCriacaoComEstadoAnteriorNulo() {
        mockarImovelECategoria();
        when(orcamentoCategoriaRepository.existsByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L)).thenReturn(false);
        when(orcamentoCategoriaRepository.save(any())).thenAnswer(chamada -> {
            OrcamentoCategoriaModel salvo = chamada.getArgument(0);
            salvo.setId(50L);
            return salvo;
        });
        when(despesaRepository.findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L)).thenReturn(List.of());

        orcamentoCategoriaService.criar(dto());

        ArgumentCaptor<OrcamentoCategoriaResponseDTO> novoCaptor = ArgumentCaptor.forClass(OrcamentoCategoriaResponseDTO.class);
        verify(auditoriaService).registrar(eq("OrcamentoCategoria"), eq(50L), eq(OperacaoAuditoria.CRIACAO), isNull(), novoCaptor.capture());
        assertThat(novoCaptor.getValue().valorOrcado()).isEqualByComparingTo("10000.00");
    }

    // Cobre .agents/rules/auditoria.md: atualizar captura o estadoAnterior antes de aplicar os
    // setters — provado comparando valorOrcado, que muda de 10.000 (orcamento(10L)) para 15.000.
    @Test
    void atualizarAuditaComEstadoAnteriorCapturadoAntesDaMutacao() {
        OrcamentoCategoriaModel existente = orcamento(10L);
        when(orcamentoCategoriaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(existente));
        when(despesaRepository.findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L)).thenReturn(List.of());
        mockarImovelECategoria();
        when(orcamentoCategoriaRepository.findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L))
                .thenReturn(Optional.of(existente));
        when(orcamentoCategoriaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        OrcamentoCategoriaRequestDTO dtoEditado = new OrcamentoCategoriaRequestDTO(1L, 2L, new BigDecimal("15000.00"), null, null);
        orcamentoCategoriaService.atualizar(10L, dtoEditado);

        ArgumentCaptor<OrcamentoCategoriaResponseDTO> anteriorCaptor = ArgumentCaptor.forClass(OrcamentoCategoriaResponseDTO.class);
        ArgumentCaptor<OrcamentoCategoriaResponseDTO> novoCaptor = ArgumentCaptor.forClass(OrcamentoCategoriaResponseDTO.class);
        verify(auditoriaService).registrar(eq("OrcamentoCategoria"), eq(10L), eq(OperacaoAuditoria.EDICAO),
                anteriorCaptor.capture(), novoCaptor.capture());

        assertThat(anteriorCaptor.getValue().valorOrcado()).isEqualByComparingTo("10000.00");
        assertThat(novoCaptor.getValue().valorOrcado()).isEqualByComparingTo("15000.00");
    }

    @Test
    void excluirAplicaExclusaoLogicaComMotivo() {
        OrcamentoCategoriaModel orcamento = orcamento(10L);
        when(orcamentoCategoriaRepository.findByIdAndAtivoTrue(10L)).thenReturn(Optional.of(orcamento));
        when(despesaRepository.findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(1L, 2L)).thenReturn(List.of());
        when(orcamentoCategoriaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        orcamentoCategoriaService.excluir(10L, "Categoria não será mais orçada");

        assertThat(orcamento.getExclusao().getAtivo()).isFalse();
        assertThat(orcamento.getExclusao().getMotivoExclusao()).isEqualTo("Categoria não será mais orçada");
        verify(auditoriaService).registrar(eq("OrcamentoCategoria"), eq(10L), eq(OperacaoAuditoria.EXCLUSAO), any(), any());
    }

    private void mockarImovelECategoria() {
        when(imovelRepository.findByIdAndAtivoTrue(1L))
                .thenReturn(Optional.of(ImovelModel.builder().id(1L).identificador("LOTE-01").build()));
        when(categoriaDespesaRepository.findById(2L))
                .thenReturn(Optional.of(CategoriaDespesaModel.builder().id(2L).nome("Material").build()));
    }

    private OrcamentoCategoriaModel orcamento(Long id) {
        return OrcamentoCategoriaModel.builder()
                .id(id)
                .imovel(ImovelModel.builder().id(1L).identificador("LOTE-01").build())
                .categoriaDespesa(CategoriaDespesaModel.builder().id(2L).nome("Material").build())
                .valorOrcado(new BigDecimal("10000.00"))
                .build();
    }

    private OrcamentoCategoriaRequestDTO dto() {
        return new OrcamentoCategoriaRequestDTO(1L, 2L, new BigDecimal("10000.00"), null, null);
    }
}
