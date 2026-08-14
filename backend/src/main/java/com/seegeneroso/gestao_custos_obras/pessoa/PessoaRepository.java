package com.seegeneroso.gestao_custos_obras.pessoa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PessoaRepository extends JpaRepository<PessoaModel, Long> {

    List<PessoaModel> findByAtivoTrue();

    Optional<PessoaModel> findByIdAndAtivoTrue(Long id);

    boolean existsByDocumento(String documento);
}
