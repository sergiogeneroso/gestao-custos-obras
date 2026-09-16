package com.seegeneroso.gestao_custos_obras.pessoa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PessoaRepository extends JpaRepository<PessoaModel, Long> {

    // Nome mantido igual ao de antes da migração para ExclusaoLogica (ADR-040) — ver o mesmo
    // comentário em ImovelRepository.
    @Query("select p from PessoaModel p where p.exclusao.ativo = true")
    List<PessoaModel> findByAtivoTrue();

    @Query("select p from PessoaModel p where p.id = :id and p.exclusao.ativo = true")
    Optional<PessoaModel> findByIdAndAtivoTrue(@Param("id") Long id);

    boolean existsByDocumento(String documento);

    /**
     * O filtro de papel chega como lista de valores aceitos ([true] para só fornecedores,
     * [true, false] para todas) em vez de um Boolean opcional: comparar um bind nulo em JPQL
     * ("param is null") faz o Postgres recusar a consulta por não inferir o tipo. Com "in" a
     * lista é sempre tipada. Mesmo motivo da busca vazia vir como "".
     */
    @Query("""
            select p from PessoaModel p
            where p.exclusao.ativo = true
              and p.fornecedor in :fornecedores
              and (lower(p.nome) like lower(concat('%', :busca, '%'))
                or lower(coalesce(p.documento, '')) like lower(concat('%', :busca, '%'))
                or lower(coalesce(p.areaAtuacao, '')) like lower(concat('%', :busca, '%')))
            """)
    Page<PessoaModel> buscar(@Param("busca") String busca,
                             @Param("fornecedores") List<Boolean> fornecedores,
                             Pageable pageable);
}
