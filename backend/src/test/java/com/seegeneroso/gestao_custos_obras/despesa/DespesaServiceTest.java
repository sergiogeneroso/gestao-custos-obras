package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.DadosCompra;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.EtapaConstrucao;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
                .ativo(true)
                .build();
    }

    private DespesaRequestDTO dto(Long imovelId, FaseImovel fase, EtapaConstrucao etapa) {
        return new DespesaRequestDTO(imovelId, 2L, 3L, null, null, fase, etapa,
                new BigDecimal("1500.00"), PAGAMENTO, "Compra de material");
    }
}
