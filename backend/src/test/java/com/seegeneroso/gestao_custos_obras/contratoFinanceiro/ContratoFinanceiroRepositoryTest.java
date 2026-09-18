package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.imovel.DadosCompra;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoPessoa;
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
 * Prova, contra o banco de teste, que {@link ContratoFinanceiroRepository#findByImovelId} só traz
 * contrato ativo (ADR-040) — nenhum consumidor (combo de despesa, cronograma, cascata de exclusão
 * do imóvel) pode ver um contrato já excluído.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContratoFinanceiroRepositoryTest {

    @Autowired private ContratoFinanceiroRepository contratoFinanceiroRepository;
    @Autowired private ImovelRepository imovelRepository;
    @Autowired private PessoaRepository pessoaRepository;

    @Test
    void findByImovelIdSoTrazContratoAtivo() {
        ImovelModel imovel = imovelRepository.save(imovel("LOTE-50"));
        PessoaModel contraparte = pessoaRepository.save(pessoa("Vendedor do Lote"));
        ContratoFinanceiroModel ativo = contratoFinanceiroRepository.save(contrato(imovel, contraparte, true));
        ContratoFinanceiroModel inativo = contratoFinanceiroRepository.save(contrato(imovel, contraparte, false));

        List<Long> ids = contratoFinanceiroRepository.findByImovelId(imovel.getId())
                .stream().map(ContratoFinanceiroModel::getId).toList();

        assertThat(ids).contains(ativo.getId()).doesNotContain(inativo.getId());
    }

    private ContratoFinanceiroModel contrato(ImovelModel imovel, PessoaModel contraparte, boolean ativo) {
        ContratoFinanceiroModel.ContratoFinanceiroModelBuilder builder = ContratoFinanceiroModel.builder()
                .imovel(imovel)
                .tipo(TipoContratoFinanceiro.PARCELAMENTO_COMPRA)
                .contraparte(contraparte)
                .valorContratado(new BigDecimal("100000.00"));
        if (!ativo) {
            builder.exclusao(ExclusaoLogica.builder().ativo(false).motivoExclusao("Excluído para teste").build());
        }
        return builder.build();
    }

    private PessoaModel pessoa(String nome) {
        return PessoaModel.builder()
                .nome(nome)
                .tipoPessoa(TipoPessoa.FISICA)
                .documento(nome + "-" + System.nanoTime())
                .build();
    }

    private ImovelModel imovel(String identificador) {
        return ImovelModel.builder()
                .identificador(identificador)
                .compra(DadosCompra.builder().data(LocalDate.of(2026, 1, 10)).build())
                .build();
    }
}
