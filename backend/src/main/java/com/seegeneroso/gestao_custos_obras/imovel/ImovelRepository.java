package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImovelRepository extends JpaRepository<ImovelModel, Long> {

    List<ImovelModel> findByAtivoTrue();

    Optional<ImovelModel> findByIdAndAtivoTrue(Long id);

    boolean existsByIdentificadorIgnoreCase(String identificador);

    // Fase e situação chegam como lista de valores aceitos — "todas" manda o enum inteiro. Ver a
    // explicação em PessoaRepository.buscar: bind nulo em JPQL não tem tipo para o Postgres.
    @Query("""
            select i from ImovelModel i
            where i.ativo = true
              and i.fase in :fases
              and i.situacao in :situacoes
              and (lower(i.identificador) like lower(concat('%', :busca, '%'))
                or lower(coalesce(i.endereco, '')) like lower(concat('%', :busca, '%')))
            """)
    Page<ImovelModel> buscar(@Param("busca") String busca,
                             @Param("fases") List<FaseImovel> fases,
                             @Param("situacoes") List<SituacaoImovel> situacoes,
                             Pageable pageable);
}
