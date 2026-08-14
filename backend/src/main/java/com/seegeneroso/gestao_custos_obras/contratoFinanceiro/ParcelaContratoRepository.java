package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcelaContratoRepository extends JpaRepository<ParcelaContratoModel, Long> {
}
