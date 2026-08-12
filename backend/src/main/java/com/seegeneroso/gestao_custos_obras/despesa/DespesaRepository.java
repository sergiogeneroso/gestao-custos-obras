package com.seegeneroso.gestao_custos_obras.despesa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DespesaRepository extends JpaRepository<DespesaModel, Long> {

    List<DespesaModel> findByImovelId(Long imovelId);

    List<DespesaModel> findByEtapaProjetoId(Long etapaProjetoId);

    List<DespesaModel> findByImovelIdAndEtapaProjetoId(Long imovelId, Long etapaProjetoId);
}
