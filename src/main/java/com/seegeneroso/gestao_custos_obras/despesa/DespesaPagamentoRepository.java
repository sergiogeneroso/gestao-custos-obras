package com.seegeneroso.gestao_custos_obras.despesa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DespesaPagamentoRepository extends JpaRepository<DespesaPagamentoModel, Long> {

    List<DespesaPagamentoModel> findByDespesaId(Long despesaId);

    List<DespesaPagamentoModel> findByAportanteId(Long aportanteId);
}
