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

    // ADR-047: contrato compartilhado aparece em findByImovelId para CADA lote vinculado, uma vez
    // só (distinct), não uma vez por linha de alocação.
    @Test
    void findByImovelIdTrazContratoCompartilhadoParaCadaLoteVinculado() {
        ImovelModel loteA = imovelRepository.save(imovel("LOTE-60"));
        ImovelModel loteB = imovelRepository.save(imovel("LOTE-61"));
        PessoaModel vendedor = pessoaRepository.save(pessoa("Vendedor dos dois lotes"));

        ContratoFinanceiroModel compartilhado = ContratoFinanceiroModel.builder()
                .tipo(TipoContratoFinanceiro.PARCELAMENTO_COMPRA)
                .contraparte(vendedor)
                .valorContratado(new BigDecimal("150000.00"))
                .build();
        compartilhado.getImoveis().add(ContratoImovelModel.builder()
                .contrato(compartilhado).imovel(loteA).valorAlocado(new BigDecimal("90000.00")).build());
        compartilhado.getImoveis().add(ContratoImovelModel.builder()
                .contrato(compartilhado).imovel(loteB).valorAlocado(new BigDecimal("60000.00")).build());
        ContratoFinanceiroModel salvo = contratoFinanceiroRepository.save(compartilhado);

        List<Long> idsParaA = contratoFinanceiroRepository.findByImovelId(loteA.getId())
                .stream().map(ContratoFinanceiroModel::getId).toList();
        List<Long> idsParaB = contratoFinanceiroRepository.findByImovelId(loteB.getId())
                .stream().map(ContratoFinanceiroModel::getId).toList();

        assertThat(idsParaA).containsExactly(salvo.getId());
        assertThat(idsParaB).containsExactly(salvo.getId());
    }

    private ContratoFinanceiroModel contrato(ImovelModel imovel, PessoaModel contraparte, boolean ativo) {
        ContratoFinanceiroModel.ContratoFinanceiroModelBuilder builder = ContratoFinanceiroModel.builder()
                .tipo(TipoContratoFinanceiro.PARCELAMENTO_COMPRA)
                .contraparte(contraparte)
                .valorContratado(new BigDecimal("100000.00"));
        if (!ativo) {
            builder.exclusao(ExclusaoLogica.builder().ativo(false).motivoExclusao("Excluído para teste").build());
        }
        ContratoFinanceiroModel contrato = builder.build();
        contrato.getImoveis().add(ContratoImovelModel.builder()
                .contrato(contrato).imovel(imovel).valorAlocado(new BigDecimal("100000.00")).build());
        return contrato;
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
