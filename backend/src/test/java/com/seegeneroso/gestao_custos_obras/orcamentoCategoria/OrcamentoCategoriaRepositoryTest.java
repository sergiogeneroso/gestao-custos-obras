package com.seegeneroso.gestao_custos_obras.orcamentoCategoria;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.DadosCompra;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.shared.exclusao.ExclusaoLogica;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra o banco de teste, que {@link OrcamentoCategoriaRepository#findByImovelId} só traz
 * linha de orçamento ativa (ADR-040) — uma linha excluída não pode reaparecer na listagem nem na
 * cascata de exclusão do imóvel.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrcamentoCategoriaRepositoryTest {

    @Autowired private OrcamentoCategoriaRepository orcamentoCategoriaRepository;
    @Autowired private ImovelRepository imovelRepository;
    @Autowired private CategoriaDespesaRepository categoriaDespesaRepository;

    // Categorias diferentes para os dois registros: uk_orcamento_imovel_categoria não deixaria
    // duas linhas ativas OU inativas do mesmo par imóvel+categoria coexistirem.
    @Test
    void findByImovelIdSoTrazOrcamentoAtivo() {
        ImovelModel imovel = imovelRepository.save(imovel("LOTE-70"));
        CategoriaDespesaModel material = categoriaDespesaRepository.save(categoria("Material"));
        CategoriaDespesaModel maoDeObra = categoriaDespesaRepository.save(categoria("Mão de obra"));
        OrcamentoCategoriaModel ativo = orcamentoCategoriaRepository.save(orcamento(imovel, material, true));
        OrcamentoCategoriaModel inativo = orcamentoCategoriaRepository.save(orcamento(imovel, maoDeObra, false));

        List<Long> ids = orcamentoCategoriaRepository.findByImovelId(imovel.getId())
                .stream().map(OrcamentoCategoriaModel::getId).toList();

        assertThat(ids).contains(ativo.getId()).doesNotContain(inativo.getId());
    }

    private OrcamentoCategoriaModel orcamento(ImovelModel imovel, CategoriaDespesaModel categoria, boolean ativo) {
        OrcamentoCategoriaModel.OrcamentoCategoriaModelBuilder builder = OrcamentoCategoriaModel.builder()
                .imovel(imovel)
                .categoriaDespesa(categoria)
                .valorOrcado(new BigDecimal("5000.00"));
        if (!ativo) {
            builder.exclusao(ExclusaoLogica.builder().ativo(false).motivoExclusao("Excluído para teste").build());
        }
        return builder.build();
    }

    private CategoriaDespesaModel categoria(String nome) {
        return CategoriaDespesaModel.builder().nome(nome).build();
    }

    private ImovelModel imovel(String identificador) {
        return ImovelModel.builder()
                .identificador(identificador)
                .compra(DadosCompra.builder().data(LocalDate.of(2026, 1, 10)).build())
                .build();
    }
}
