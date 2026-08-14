package com.seegeneroso.gestao_custos_obras.orcamentoCategoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrcamentoCategoriaRepository extends JpaRepository<OrcamentoCategoriaModel, Long> {

    List<OrcamentoCategoriaModel> findByImovelId(Long imovelId);

    Optional<OrcamentoCategoriaModel> findByImovelIdAndCategoriaDespesaId(Long imovelId, Long categoriaDespesaId);

    boolean existsByImovelIdAndCategoriaDespesaId(Long imovelId, Long categoriaDespesaId);
}
