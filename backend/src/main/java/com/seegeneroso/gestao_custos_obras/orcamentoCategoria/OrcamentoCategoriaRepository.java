package com.seegeneroso.gestao_custos_obras.orcamentoCategoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrcamentoCategoriaRepository extends JpaRepository<OrcamentoCategoriaModel, Long> {

    // Só ativos: uma linha de orçamento excluída não deve reaparecer na listagem nem na cascata
    // de exclusão do imóvel.
    @Query("select o from OrcamentoCategoriaModel o where o.imovel.id = :imovelId and o.exclusao.ativo = true")
    List<OrcamentoCategoriaModel> findByImovelId(@Param("imovelId") Long imovelId);

    @Query("select o from OrcamentoCategoriaModel o where o.id = :id and o.exclusao.ativo = true")
    Optional<OrcamentoCategoriaModel> findByIdAndAtivoTrue(@Param("id") Long id);

    // Bug corrigido (ADR-041, issue 04): sem o filtro de ativo, um orçamento excluído continuava
    // contando para a checagem de duplicidade e bloqueava indefinidamente um novo lançamento para
    // a mesma categoria.
    @Query("""
            select o from OrcamentoCategoriaModel o
            where o.imovel.id = :imovelId and o.categoriaDespesa.id = :categoriaDespesaId
              and o.exclusao.ativo = true
            """)
    Optional<OrcamentoCategoriaModel> findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(
            @Param("imovelId") Long imovelId, @Param("categoriaDespesaId") Long categoriaDespesaId);

    @Query("""
            select case when count(o) > 0 then true else false end from OrcamentoCategoriaModel o
            where o.imovel.id = :imovelId and o.categoriaDespesa.id = :categoriaDespesaId
              and o.exclusao.ativo = true
            """)
    boolean existsByImovelIdAndCategoriaDespesaIdAndAtivoTrue(
            @Param("imovelId") Long imovelId, @Param("categoriaDespesaId") Long categoriaDespesaId);
}
