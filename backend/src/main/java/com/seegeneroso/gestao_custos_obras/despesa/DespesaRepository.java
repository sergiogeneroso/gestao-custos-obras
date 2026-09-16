package com.seegeneroso.gestao_custos_obras.despesa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DespesaRepository extends JpaRepository<DespesaModel, Long> {

    // Nomes mantidos iguais aos de antes da migração para ExclusaoLogica (ADR-040) — ver o mesmo
    // comentário em ImovelRepository.
    @Query("select d from DespesaModel d where d.exclusao.ativo = true")
    List<DespesaModel> findByAtivoTrue();

    @Query("select d from DespesaModel d where d.id = :id and d.exclusao.ativo = true")
    Optional<DespesaModel> findByIdAndAtivoTrue(@Param("id") Long id);

    @Query("select d from DespesaModel d where d.imovel.id = :imovelId and d.exclusao.ativo = true")
    List<DespesaModel> findByImovelIdAndAtivoTrue(@Param("imovelId") Long imovelId);

    @Query("select d from DespesaModel d where d.categoriaDespesa.id = :categoriaDespesaId and d.exclusao.ativo = true")
    List<DespesaModel> findByCategoriaDespesaIdAndAtivoTrue(@Param("categoriaDespesaId") Long categoriaDespesaId);

    @Query("""
            select d from DespesaModel d
            where d.imovel.id = :imovelId and d.categoriaDespesa.id = :categoriaDespesaId
              and d.exclusao.ativo = true
            """)
    List<DespesaModel> findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(
            @Param("imovelId") Long imovelId, @Param("categoriaDespesaId") Long categoriaDespesaId);

    @Query("select d from DespesaModel d where d.imovel is null and d.exclusao.ativo = true")
    List<DespesaModel> findByImovelIsNullAndAtivoTrue();

    // Usado pela cascata de exclusão de contrato (ContratoFinanceiroService.cascatearExclusao)
    // para desvincular despesas de custo acessório do contrato excluído.
    List<DespesaModel> findByContratoFinanceiroId(Long contratoFinanceiroId);

    /**
     * Busca paginada da tela de despesas. O termo varre os mesmos campos que a tela filtrava no
     * navegador — inclusive os nomes que vêm por associação, daí os left joins (imóvel e
     * beneficiário são opcionais).
     *
     * O escopo chega como texto comparado a literais ('TODAS', 'IMOVEL', 'GERAL') porque a
     * comparação com literal dá ao Hibernate o tipo do bind; ":param is null" não daria, e o
     * Postgres recusaria a consulta.
     */
    @Query("""
            select d from DespesaModel d
            left join d.imovel i
            join d.categoriaDespesa c
            join d.pagador pg
            left join d.beneficiario bf
            where d.exclusao.ativo = true
              and (:escopo = 'TODAS'
                or (:escopo = 'IMOVEL' and d.imovel is not null)
                or (:escopo = 'GERAL' and d.imovel is null))
              and (lower(c.nome) like lower(concat('%', :busca, '%'))
                or lower(pg.nome) like lower(concat('%', :busca, '%'))
                or lower(coalesce(bf.nome, '')) like lower(concat('%', :busca, '%'))
                or lower(coalesce(i.identificador, '')) like lower(concat('%', :busca, '%'))
                or lower(coalesce(d.descricao, '')) like lower(concat('%', :busca, '%')))
            """)
    Page<DespesaModel> buscar(@Param("busca") String busca,
                              @Param("escopo") String escopo,
                              Pageable pageable);
}
