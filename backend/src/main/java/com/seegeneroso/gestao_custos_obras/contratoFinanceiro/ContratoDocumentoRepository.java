package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContratoDocumentoRepository extends JpaRepository<ContratoDocumentoModel, Long> {

    List<ContratoDocumentoModel> findByContratoId(Long contratoId);
}
