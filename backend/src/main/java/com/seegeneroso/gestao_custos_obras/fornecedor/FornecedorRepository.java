package com.seegeneroso.gestao_custos_obras.fornecedor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FornecedorRepository extends JpaRepository<FornecedorModel, Long> {

    List<FornecedorModel> findByAtivoTrue();

    Optional<FornecedorModel> findByIdAndAtivoTrue(Long id);

    boolean existsByPessoaId(Long pessoaId);
}
