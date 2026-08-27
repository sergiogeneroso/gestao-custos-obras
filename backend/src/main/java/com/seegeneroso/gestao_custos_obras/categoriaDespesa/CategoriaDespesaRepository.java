package com.seegeneroso.gestao_custos_obras.categoriaDespesa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaDespesaRepository extends JpaRepository<CategoriaDespesaModel, Long> {

    Optional<CategoriaDespesaModel> findByNome(String nome);

    // Busca vazia chega como "" e vira like '%%', que casa com tudo. É o que evita escrever
    // ":busca is null" em JPQL, que o Postgres recusa por não conseguir inferir o tipo do bind.
    @Query("""
            select c from CategoriaDespesaModel c
            where lower(c.nome) like lower(concat('%', :busca, '%'))
               or lower(coalesce(c.descricao, '')) like lower(concat('%', :busca, '%'))
            """)
    Page<CategoriaDespesaModel> buscar(@Param("busca") String busca, Pageable pageable);
}
