package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContratoFinanceiroRepository extends JpaRepository<ContratoFinanceiroModel, Long> {

    List<ContratoFinanceiroModel> findByImovelId(Long imovelId);

    // O termo varre imóvel e contraparte, os mesmos campos que a tela filtrava no navegador.
    // Contraparte é opcional, daí o left join. Busca vazia chega como "" e casa com tudo.
    @Query("""
            select c from ContratoFinanceiroModel c
            join c.imovel i
            left join c.contraparte p
            where lower(i.identificador) like lower(concat('%', :busca, '%'))
               or lower(coalesce(p.nome, '')) like lower(concat('%', :busca, '%'))
            """)
    Page<ContratoFinanceiroModel> buscar(@Param("busca") String busca, Pageable pageable);
}
