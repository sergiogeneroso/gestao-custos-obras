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

    Optional<OrcamentoCategoriaModel> findByImovelIdAndCategoriaDespesaId(Long imovelId, Long categoriaDespesaId);

    boolean existsByImovelIdAndCategoriaDespesaId(Long imovelId, Long categoriaDespesaId);
}
