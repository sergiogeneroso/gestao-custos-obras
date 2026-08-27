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

    List<DespesaModel> findByAtivoTrue();

    Optional<DespesaModel> findByIdAndAtivoTrue(Long id);

    List<DespesaModel> findByImovelIdAndAtivoTrue(Long imovelId);

    List<DespesaModel> findByCategoriaDespesaIdAndAtivoTrue(Long categoriaDespesaId);

    List<DespesaModel> findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(Long imovelId, Long categoriaDespesaId);

    List<DespesaModel> findByImovelIsNullAndAtivoTrue();

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
            where d.ativo = true
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
