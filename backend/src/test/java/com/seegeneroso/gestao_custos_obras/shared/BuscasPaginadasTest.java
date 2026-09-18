package com.seegeneroso.gestao_custos_obras.shared;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.imovel.DadosCompra;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoPessoa;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As consultas paginadas usam @Query com JPQL, e o contexto do Spring só as **parseia** ao subir.
 * O que quebra de verdade — bind de parâmetro cujo tipo o Postgres não consegue inferir — só
 * aparece na execução. Este teste executa cada uma contra o banco, com e sem termo de busca, e
 * verifica o conteúdo real devolvido (não só "não é nulo"), para que essa classe de erro não
 * chegue à tela.
 *
 * Os filtros AtivoTrue e o filtro de fase/situação/escopo de despesa e imóvel têm teste próprio,
 * lado a lado com um registro inativo, em {@code DespesaRepositoryTest} e
 * {@code ImovelRepositoryTest} — não duplicados aqui.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BuscasPaginadasTest {

    @Autowired private PessoaRepository pessoaRepository;
    @Autowired private ImovelRepository imovelRepository;
    @Autowired private ContratoFinanceiroRepository contratoFinanceiroRepository;
    @Autowired private CategoriaDespesaRepository categoriaDespesaRepository;

    private static final PageRequest PAGINA = PageRequest.of(0, 20, Sort.by("id"));

    @Test
    void pessoaBuscaPorTermoERespeitaFiltroDeFornecedor() {
        PessoaModel fornecedor = pessoaRepository.save(pessoa("Silva Fornecedor", true));
        PessoaModel comprador = pessoaRepository.save(pessoa("Souza Comprador", false));

        assertThat(pessoaRepository.buscar("", List.of(true, false), PAGINA).getContent())
                .extracting(PessoaModel::getId)
                .contains(fornecedor.getId(), comprador.getId());
        assertThat(pessoaRepository.buscar("silva", List.of(true, false), PAGINA).getContent())
                .extracting(PessoaModel::getId)
                .containsExactly(fornecedor.getId());
        // Nome bate, mas o filtro de papel exclui: fornecedor=true não está na lista aceita.
        assertThat(pessoaRepository.buscar("silva", List.of(false), PAGINA).getContent()).isEmpty();
    }

    @Test
    void contratoBuscaPorTermoDeImovelOuContraparte() {
        ImovelModel imovel = imovelRepository.save(imovel("LOTE-90"));
        PessoaModel banco = pessoaRepository.save(pessoa("Banco Caixa", false));
        ContratoFinanceiroModel contrato = contratoFinanceiroRepository.save(
                ContratoFinanceiroModel.builder()
                        .imovel(imovel)
                        .tipo(TipoContratoFinanceiro.FINANCIAMENTO_CONSTRUCAO)
                        .contraparte(banco)
                        .valorContratado(new BigDecimal("200000.00"))
                        .build());

        assertThat(contratoFinanceiroRepository.buscar("", PAGINA).getContent())
                .extracting(ContratoFinanceiroModel::getId).contains(contrato.getId());
        assertThat(contratoFinanceiroRepository.buscar("caixa", PAGINA).getContent())
                .extracting(ContratoFinanceiroModel::getId).containsExactly(contrato.getId());
        assertThat(contratoFinanceiroRepository.buscar("termo-inexistente-xyz", PAGINA).getContent()).isEmpty();
    }

    // Nome escolhido para não colidir com o seed padrão (CategoriaDespesaSeedRunner: "Material",
    // "Mão de obra" etc. já existem no banco de teste, criados fora da transação do teste).
    @Test
    void categoriaBuscaPorTermoDeNome() {
        CategoriaDespesaModel categoria = categoriaDespesaRepository.save(
                CategoriaDespesaModel.builder().nome("Categoria de Teste Xpto").build());

        assertThat(categoriaDespesaRepository.buscar("", PAGINA).getContent())
                .extracting(CategoriaDespesaModel::getId).contains(categoria.getId());
        assertThat(categoriaDespesaRepository.buscar("xpto", PAGINA).getContent())
                .extracting(CategoriaDespesaModel::getId).containsExactly(categoria.getId());
        assertThat(categoriaDespesaRepository.buscar("termo-inexistente-xyz", PAGINA).getContent()).isEmpty();
    }

    private PessoaModel pessoa(String nome, boolean fornecedor) {
        return PessoaModel.builder()
                .nome(nome)
                .tipoPessoa(TipoPessoa.FISICA)
                .documento(nome + "-" + System.nanoTime())
                .fornecedor(fornecedor)
                .build();
    }

    private ImovelModel imovel(String identificador) {
        return ImovelModel.builder()
                .identificador(identificador)
                .compra(DadosCompra.builder().data(LocalDate.of(2026, 1, 10)).build())
                .build();
    }
}
