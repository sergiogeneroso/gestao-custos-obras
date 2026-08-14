package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContratoFinanceiroRepository extends JpaRepository<ContratoFinanceiroModel, Long> {

    List<ContratoFinanceiroModel> findByImovelId(Long imovelId);
}
