package com.seegeneroso.gestao_custos_obras.etapaProjeto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EtapaProjetoRepository extends JpaRepository<EtapaProjetoModel, Long> {

    Optional<EtapaProjetoModel> findByNome(String nome);
}
