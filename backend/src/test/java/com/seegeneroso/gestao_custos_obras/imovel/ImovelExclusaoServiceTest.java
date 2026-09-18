package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroService;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImpactoExclusaoImovelResponseDTO;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.OrcamentoCategoriaModel;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.OrcamentoCategoriaRepository;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import com.seegeneroso.gestao_custos_obras.shared.storage.ArquivoUrls;
import com.seegeneroso.gestao_custos_obras.shared.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cobre a ADR-040: a exclusão do imóvel cascateia mesmo sobre contrato QUITADO/parcela paga,
// diferente da exclusão avulsa de contrato (que tem trava). Ver
// .agents/rules/regras-negocio-financeiras.md.
@ExtendWith(MockitoExtension.class)
class ImovelExclusaoServiceTest {

    @Mock
    private ImovelRepository imovelRepository;
    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private ContratoFinanceiroRepository contratoFinanceiroRepository;
    @Mock
    private ContratoFinanceiroService contratoFinanceiroService;
    @Mock
    private OrcamentoCategoriaRepository orcamentoCategoriaRepository;
    @Mock
    private ImovelFotoRepository imovelFotoRepository;
    @Mock
    private ImovelDocumentoRepository imovelDocumentoRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private UsuarioAutenticadoService usuarioAutenticadoService;
    @Mock
    private ImovelMapper imovelMapper;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ImovelExclusaoService service;

    @Test
    void excluirCascateiaContratoQuitadoDespesaOrcamentoFotosEDocumentos() {
        ImovelModel imovel = ImovelModel.builder().id(1L).identificador("LOTE-01").build();
        ContratoFinanceiroModel contratoQuitado = ContratoFinanceiroModel.builder()
                .id(2L).imovel(imovel).tipo(TipoContratoFinanceiro.FINANCIAMENTO_CONSTRUCAO)
                .situacao(SituacaoContrato.QUITADO).build();
        DespesaModel despesa = DespesaModel.builder().id(3L).imovel(imovel).build();
        OrcamentoCategoriaModel orcamento = OrcamentoCategoriaModel.builder().id(4L).imovel(imovel).build();
        ImovelFotoModel foto = ImovelFotoModel.builder().id(5L).imovel(imovel).url("imoveis/1/foto.jpg").build();
        ImovelDocumentoModel documento = ImovelDocumentoModel.builder().id(6L).imovel(imovel).url("imoveis/1/documentos/doc.pdf").build();

        when(imovelRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(imovel));
        when(usuarioAutenticadoService.usuarioAtual()).thenReturn(UsuarioModel.builder().id(9L).build());
        when(contratoFinanceiroRepository.findByImovelId(1L)).thenReturn(List.of(contratoQuitado));
        when(despesaRepository.findByImovelIdAndAtivoTrue(1L)).thenReturn(List.of(despesa));
        when(orcamentoCategoriaRepository.findByImovelId(1L)).thenReturn(List.of(orcamento));
        when(imovelFotoRepository.findByImovelId(1L)).thenReturn(List.of(foto));
        when(imovelDocumentoRepository.findByImovelId(1L)).thenReturn(List.of(documento));

        service.excluir(1L, "excluir lote inteiro");

        // Contrato quitado é cascateado mesmo assim — sem checar a trava que só vale para o
        // endpoint avulso (ContratoFinanceiroService.excluir).
        verify(contratoFinanceiroService).cascatearExclusao(contratoQuitado, "excluir lote inteiro", imovel.getExclusao().getExcluidoPor());
        assertThat(despesa.getExclusao().getAtivo()).isFalse();
        assertThat(orcamento.getExclusao().getAtivo()).isFalse();
        assertThat(imovel.getExclusao().getAtivo()).isFalse();
        verify(imovelFotoRepository).delete(foto);
        verify(imovelDocumentoRepository).delete(documento);
        verify(storageService, org.mockito.Mockito.times(2)).deletar(any(), any());
        verify(imovelRepository).save(imovel);
    }

    @Test
    void calcularImpactoContaRegistrosVinculados() {
        when(imovelRepository.existsById(1L)).thenReturn(true);
        when(despesaRepository.findByImovelIdAndAtivoTrue(1L)).thenReturn(List.of(new DespesaModel(), new DespesaModel()));
        when(contratoFinanceiroRepository.findByImovelId(1L)).thenReturn(List.of(new ContratoFinanceiroModel()));
        when(imovelFotoRepository.findByImovelId(1L)).thenReturn(List.of());
        when(imovelDocumentoRepository.findByImovelId(1L)).thenReturn(List.of(new ImovelDocumentoModel()));
        when(orcamentoCategoriaRepository.findByImovelId(1L)).thenReturn(List.of());

        ImpactoExclusaoImovelResponseDTO impacto = service.calcularImpacto(1L);

        assertThat(impacto.despesas()).isEqualTo(2);
        assertThat(impacto.contratosFinanceiros()).isEqualTo(1);
        assertThat(impacto.fotos()).isEqualTo(0);
        assertThat(impacto.documentos()).isEqualTo(1);
        assertThat(impacto.orcamentosCategoria()).isEqualTo(0);
    }
}
