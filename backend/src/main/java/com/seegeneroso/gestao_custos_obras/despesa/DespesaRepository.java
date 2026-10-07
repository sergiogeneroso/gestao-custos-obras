package com.seegeneroso.gestao_custos_obras.despesa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DespesaRepository extends JpaRepository<DespesaModel, Long>, JpaSpecificationExecutor<DespesaModel>,
        DespesaRepositoryCustom {

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
}
