package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoAnexoDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DespesaAnexoRepository extends JpaRepository<DespesaAnexoModel, Long> {

    List<DespesaAnexoModel> findByDespesaId(Long despesaId);

    List<DespesaAnexoModel> findByDespesaIdAndTipoAnexo(Long despesaId, TipoAnexoDespesa tipoAnexo);

    /**
     * Ids de despesa repetidos, um por anexo, para a tela contar quantos comprovantes cada
     * lançamento tem. Agrupar em Java em vez de no banco porque a lista vem sempre limitada aos
     * ids de uma página da busca — dezenas de linhas, não a tabela toda.
     */
    @Query("select a.despesa.id from DespesaAnexoModel a where a.despesa.id in :despesaIds")
    List<Long> listarDespesaIdPorAnexo(@Param("despesaIds") List<Long> despesaIds);
}
