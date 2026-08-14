package com.seegeneroso.gestao_custos_obras.categoriaDespesa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaDespesaRepository extends JpaRepository<CategoriaDespesaModel, Long> {

    Optional<CategoriaDespesaModel> findByNome(String nome);
}
