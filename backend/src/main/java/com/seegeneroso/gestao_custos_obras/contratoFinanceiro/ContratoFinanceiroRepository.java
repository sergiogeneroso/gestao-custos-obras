package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContratoFinanceiroRepository extends JpaRepository<ContratoFinanceiroModel, Long> {

    // Só ativos: nenhum consumidor (combo de despesa, conferência de cronograma, aviso de
    // parcelamento ativo, cascata de exclusão do imóvel) precisa ver um contrato já excluído.
    @Query("select c from ContratoFinanceiroModel c where c.imovel.id = :imovelId and c.exclusao.ativo = true")
    List<ContratoFinanceiroModel> findByImovelId(@Param("imovelId") Long imovelId);

    @Query("select c from ContratoFinanceiroModel c where c.id = :id and c.exclusao.ativo = true")
    Optional<ContratoFinanceiroModel> findByIdAndAtivoTrue(@Param("id") Long id);

    // O termo varre imóvel e contraparte, os mesmos campos que a tela filtrava no navegador.
    // Contraparte é opcional, daí o left join. Busca vazia chega como "" e casa com tudo.
    @Query("""
            select c from ContratoFinanceiroModel c
            join c.imovel i
            left join c.contraparte p
            where c.exclusao.ativo = true
              and (lower(i.identificador) like lower(concat('%', :busca, '%'))
               or lower(coalesce(p.nome, '')) like lower(concat('%', :busca, '%')))
            """)
    Page<ContratoFinanceiroModel> buscar(@Param("busca") String busca, Pageable pageable);
}
