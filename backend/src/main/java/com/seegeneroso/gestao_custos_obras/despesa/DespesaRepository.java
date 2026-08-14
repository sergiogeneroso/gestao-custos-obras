package com.seegeneroso.gestao_custos_obras.despesa;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
