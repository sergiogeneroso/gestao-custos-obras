package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import com.seegeneroso.gestao_custos_obras.shared.exclusao.ExclusaoLogica;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra o banco de teste, que {@link ImovelRepository#findByAtivoTrue} e
 * {@link ImovelRepository#buscar} não trazem imóvel excluído (ADR-040) e que `buscar` respeita o
 * filtro de fase/situação da tela — imóvel de outra fase/situação não pode vazar num resultado
 * filtrado.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImovelRepositoryTest {

    @Autowired private ImovelRepository imovelRepository;

    private static final PageRequest PAGINA = PageRequest.of(0, 20);

    @Test
    void findByAtivoTrueSoTrazImovelAtivo() {
        ImovelModel ativo = imovelRepository.save(imovel("LOTE-60", FaseImovel.LOTE, SituacaoImovel.ADQUIRIDO, true));
        ImovelModel inativo = imovelRepository.save(imovel("LOTE-61", FaseImovel.LOTE, SituacaoImovel.ADQUIRIDO, false));

        List<Long> ids = imovelRepository.findByAtivoTrue().stream().map(ImovelModel::getId).toList();

        assertThat(ids).contains(ativo.getId()).doesNotContain(inativo.getId());
    }

    @Test
    void buscarSoTrazImovelAtivoDaFaseESituacaoFiltradas() {
        ImovelModel loteAdquirido = imovelRepository.save(imovel("LOTE-62", FaseImovel.LOTE, SituacaoImovel.ADQUIRIDO, true));
        ImovelModel casaVendida = imovelRepository.save(imovel("CASA-01", FaseImovel.CASA, SituacaoImovel.VENDIDO, true));
        ImovelModel loteInativo = imovelRepository.save(imovel("LOTE-63", FaseImovel.LOTE, SituacaoImovel.ADQUIRIDO, false));

        Page<ImovelModel> resultado = imovelRepository.buscar(
                "", List.of(FaseImovel.LOTE), List.of(SituacaoImovel.ADQUIRIDO), PAGINA);

        assertThat(resultado.getContent()).extracting(ImovelModel::getId)
                .contains(loteAdquirido.getId())
                .doesNotContain(casaVendida.getId(), loteInativo.getId());
    }

    private ImovelModel imovel(String identificador, FaseImovel fase, SituacaoImovel situacao, boolean ativo) {
        ImovelModel.ImovelModelBuilder builder = ImovelModel.builder()
                .identificador(identificador)
                .fase(fase)
                .situacao(situacao)
                .compra(DadosCompra.builder().data(LocalDate.of(2026, 1, 10)).build());
        if (!ativo) {
            builder.exclusao(ExclusaoLogica.builder().ativo(false).motivoExclusao("Excluído para teste").build());
        }
        return builder.build();
    }
}
