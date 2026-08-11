package com.seegeneroso.gestao_custos_obras.orcamentoEtapa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrcamentoEtapaRepository extends JpaRepository<OrcamentoEtapaModel, Long> {

    List<OrcamentoEtapaModel> findByImovelId(Long imovelId);

    Optional<OrcamentoEtapaModel> findByImovelIdAndEtapaProjetoId(Long imovelId, Long etapaProjetoId);

    boolean existsByImovelIdAndEtapaProjetoId(Long imovelId, Long etapaProjetoId);
}
