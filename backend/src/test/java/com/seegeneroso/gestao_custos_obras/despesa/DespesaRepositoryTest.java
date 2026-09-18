package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.DadosCompra;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoAnexoDespesa;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoPessoa;
import com.seegeneroso.gestao_custos_obras.shared.exclusao.ExclusaoLogica;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova, contra o banco de teste, que os filtros AtivoTrue de {@link DespesaRepository} (ADR-040,
 * regras-negocio-financeiras.md) excluem despesa logicamente excluída das consultas que alimentam
 * o custo do imóvel — sem esse filtro um lançamento excluído continuaria pesando no resultado.
 * Cada teste monta um registro ativo e um inativo lado a lado. Também cobre
 * {@link DespesaAnexoRepository#listarDespesaIdPorAnexoDoTipo} (ADR-023: só COMPROVANTE prova o
 * pagamento), fundido aqui por viver no mesmo pacote e ter um único cenário.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DespesaRepositoryTest {

    @Autowired private DespesaRepository despesaRepository;
    @Autowired private DespesaAnexoRepository despesaAnexoRepository;
    @Autowired private ImovelRepository imovelRepository;
    @Autowired private PessoaRepository pessoaRepository;
    @Autowired private CategoriaDespesaRepository categoriaDespesaRepository;

    private static final PageRequest PAGINA = PageRequest.of(0, 20);

    @Test
    void findByAtivoTrueSoTrazDespesaAtiva() {
        PessoaModel pagador = pessoaRepository.save(pessoa("Pagador"));
        CategoriaDespesaModel categoria = categoriaDespesaRepository.save(categoria("Material"));
        DespesaModel ativa = despesaRepository.save(despesa(null, pagador, categoria, true));
        DespesaModel inativa = despesaRepository.save(despesa(null, pagador, categoria, false));

        List<Long> ids = despesaRepository.findByAtivoTrue().stream().map(DespesaModel::getId).toList();

        assertThat(ids).contains(ativa.getId()).doesNotContain(inativa.getId());
    }

    @Test
    void findByImovelIdAndAtivoTrueSoTrazDespesaAtivaDoImovel() {
        ImovelModel imovel = imovelRepository.save(imovel("LOTE-01"));
        PessoaModel pagador = pessoaRepository.save(pessoa("Pagador"));
        CategoriaDespesaModel categoria = categoriaDespesaRepository.save(categoria("Material"));
        DespesaModel ativa = despesaRepository.save(despesa(imovel, pagador, categoria, true));
        DespesaModel inativa = despesaRepository.save(despesa(imovel, pagador, categoria, false));

        List<Long> ids = despesaRepository.findByImovelIdAndAtivoTrue(imovel.getId())
                .stream().map(DespesaModel::getId).toList();

        assertThat(ids).contains(ativa.getId()).doesNotContain(inativa.getId());
    }

    // Prova as duas condições da query: só ativa, e só quem não tem imóvel (gasto geral).
    @Test
    void findByImovelIsNullAndAtivoTrueSoTrazDespesaAtivaSemImovel() {
        ImovelModel imovel = imovelRepository.save(imovel("LOTE-02"));
        PessoaModel pagador = pessoaRepository.save(pessoa("Pagador"));
        CategoriaDespesaModel categoria = categoriaDespesaRepository.save(categoria("Material"));
        DespesaModel geralAtiva = despesaRepository.save(despesa(null, pagador, categoria, true));
        DespesaModel doImovel = despesaRepository.save(despesa(imovel, pagador, categoria, true));
        DespesaModel geralInativa = despesaRepository.save(despesa(null, pagador, categoria, false));

        List<Long> ids = despesaRepository.findByImovelIsNullAndAtivoTrue()
                .stream().map(DespesaModel::getId).toList();

        assertThat(ids).contains(geralAtiva.getId()).doesNotContain(doImovel.getId(), geralInativa.getId());
    }

    @Test
    void findByImovelIdAndCategoriaDespesaIdAndAtivoTrueSoTrazDespesaAtivaDaCategoria() {
        ImovelModel imovel = imovelRepository.save(imovel("LOTE-03"));
        PessoaModel pagador = pessoaRepository.save(pessoa("Pagador"));
        CategoriaDespesaModel material = categoriaDespesaRepository.save(categoria("Material"));
        CategoriaDespesaModel maoDeObra = categoriaDespesaRepository.save(categoria("Mão de obra"));
        DespesaModel ativaMaterial = despesaRepository.save(despesa(imovel, pagador, material, true));
        DespesaModel inativaMaterial = despesaRepository.save(despesa(imovel, pagador, material, false));
        DespesaModel ativaOutraCategoria = despesaRepository.save(despesa(imovel, pagador, maoDeObra, true));

        List<Long> ids = despesaRepository
                .findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(imovel.getId(), material.getId())
                .stream().map(DespesaModel::getId).toList();

        assertThat(ids).contains(ativaMaterial.getId())
                .doesNotContain(inativaMaterial.getId(), ativaOutraCategoria.getId());
    }

    @Test
    void buscarSoTrazDespesaAtivaERespeitaEscopo() {
        ImovelModel imovel = imovelRepository.save(imovel("LOTE-04"));
        PessoaModel pagador = pessoaRepository.save(pessoa("Pagador"));
        CategoriaDespesaModel categoria = categoriaDespesaRepository.save(categoria("Material"));
        DespesaModel doImovelAtiva = despesaRepository.save(despesa(imovel, pagador, categoria, true));
        DespesaModel geralAtiva = despesaRepository.save(despesa(null, pagador, categoria, true));
        DespesaModel doImovelInativa = despesaRepository.save(despesa(imovel, pagador, categoria, false));

        Page<DespesaModel> todas = despesaRepository.buscar("", "TODAS", PAGINA);
        assertThat(todas.getContent()).extracting(DespesaModel::getId)
                .contains(doImovelAtiva.getId(), geralAtiva.getId())
                .doesNotContain(doImovelInativa.getId());

        Page<DespesaModel> soImovel = despesaRepository.buscar("", "IMOVEL", PAGINA);
        assertThat(soImovel.getContent()).extracting(DespesaModel::getId)
                .contains(doImovelAtiva.getId())
                .doesNotContain(geralAtiva.getId(), doImovelInativa.getId());

        Page<DespesaModel> soGeral = despesaRepository.buscar("", "GERAL", PAGINA);
        assertThat(soGeral.getContent()).extracting(DespesaModel::getId)
                .contains(geralAtiva.getId())
                .doesNotContain(doImovelAtiva.getId());
    }

    // ADR-023: RECIBO/NOTA_FISCAL/outros tipos não provam pagamento — só COMPROVANTE conta.
    @Test
    void listarDespesaIdPorAnexoDoTipoSoTrazAnexoDoTipoComprovante() {
        PessoaModel pagador = pessoaRepository.save(pessoa("Pagador"));
        CategoriaDespesaModel categoria = categoriaDespesaRepository.save(categoria("Material"));
        DespesaModel comComprovante = despesaRepository.save(despesa(null, pagador, categoria, true));
        DespesaModel comRecibo = despesaRepository.save(despesa(null, pagador, categoria, true));
        despesaAnexoRepository.save(anexo(comComprovante, TipoAnexoDespesa.COMPROVANTE));
        despesaAnexoRepository.save(anexo(comRecibo, TipoAnexoDespesa.RECIBO));

        List<Long> ids = despesaAnexoRepository.listarDespesaIdPorAnexoDoTipo(
                List.of(comComprovante.getId(), comRecibo.getId()), TipoAnexoDespesa.COMPROVANTE);

        assertThat(ids).containsExactly(comComprovante.getId());
    }

    private DespesaModel despesa(ImovelModel imovel, PessoaModel pagador, CategoriaDespesaModel categoria, boolean ativo) {
        DespesaModel.DespesaModelBuilder builder = DespesaModel.builder()
                .imovel(imovel)
                .categoriaDespesa(categoria)
                .pagador(pagador)
                .valor(new BigDecimal("100.00"))
                .dataPagamento(LocalDate.of(2026, 1, 10));
        if (!ativo) {
            builder.exclusao(ExclusaoLogica.builder().ativo(false).motivoExclusao("Excluído para teste").build());
        }
        return builder.build();
    }

    private DespesaAnexoModel anexo(DespesaModel despesa, TipoAnexoDespesa tipo) {
        return DespesaAnexoModel.builder().despesa(despesa).tipoAnexo(tipo).url("http://arquivos.local/anexo.pdf").build();
    }

    private PessoaModel pessoa(String nome) {
        return PessoaModel.builder()
                .nome(nome)
                .tipoPessoa(TipoPessoa.FISICA)
                .documento(nome + "-" + System.nanoTime())
                .build();
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
