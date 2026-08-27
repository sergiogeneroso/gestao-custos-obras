package com.seegeneroso.gestao_custos_obras.shared;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As consultas paginadas usam @Query com JPQL, e o contexto do Spring só as **parseia** ao subir.
 * O que quebra de verdade — bind de parâmetro cujo tipo o Postgres não consegue inferir — só
 * aparece na execução. Este teste executa cada uma contra o banco, com e sem termo de busca, para
 * que essa classe de erro não chegue à tela.
 */
@SpringBootTest
@Transactional(readOnly = true)
class BuscasPaginadasTest {

    @Autowired private PessoaRepository pessoaRepository;
    @Autowired private ImovelRepository imovelRepository;
    @Autowired private DespesaRepository despesaRepository;
    @Autowired private ContratoFinanceiroRepository contratoFinanceiroRepository;
    @Autowired private CategoriaDespesaRepository categoriaDespesaRepository;

    private static final PageRequest PAGINA = PageRequest.of(0, 20, Sort.by("id"));

    @Test
    void pessoaBuscaComTermoESemTermoEComFiltroDePapel() {
        assertThat(pessoaRepository.buscar("", List.of(true, false), PAGINA)).isNotNull();
        assertThat(pessoaRepository.buscar("silva", List.of(true), PAGINA)).isNotNull();
    }

    @Test
    void imovelBuscaComTodosOsValoresDoEnumEComUmSo() {
        assertThat(imovelRepository.buscar("", List.of(FaseImovel.values()), List.of(SituacaoImovel.values()), PAGINA))
                .isNotNull();
        assertThat(imovelRepository.buscar("lote", List.of(FaseImovel.LOTE), List.of(SituacaoImovel.ADQUIRIDO), PAGINA))
                .isNotNull();
    }

    @Test
    void despesaBuscaNosTresEscopos() {
        assertThat(despesaRepository.buscar("", "TODAS", PAGINA)).isNotNull();
        assertThat(despesaRepository.buscar("material", "IMOVEL", PAGINA)).isNotNull();
        assertThat(despesaRepository.buscar("", "GERAL", PAGINA)).isNotNull();
    }

    @Test
    void contratoECategoriaBuscamComESemTermo() {
        assertThat(contratoFinanceiroRepository.buscar("", PAGINA)).isNotNull();
        assertThat(contratoFinanceiroRepository.buscar("lote", PAGINA)).isNotNull();
        assertThat(categoriaDespesaRepository.buscar("", PAGINA)).isNotNull();
        assertThat(categoriaDespesaRepository.buscar("material", PAGINA)).isNotNull();
    }
}
