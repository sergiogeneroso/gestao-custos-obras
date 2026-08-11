package com.seegeneroso.gestao_custos_obras.aportante;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AportanteRepository extends JpaRepository<AportanteModel, Long> {

    List<AportanteModel> findByAtivoTrue();

    Optional<AportanteModel> findByIdAndAtivoTrue(Long id);
}
