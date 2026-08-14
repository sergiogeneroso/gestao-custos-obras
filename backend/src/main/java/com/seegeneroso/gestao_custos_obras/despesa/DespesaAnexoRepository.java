package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoAnexoDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DespesaAnexoRepository extends JpaRepository<DespesaAnexoModel, Long> {

    List<DespesaAnexoModel> findByDespesaId(Long despesaId);

    List<DespesaAnexoModel> findByDespesaIdAndTipoAnexo(Long despesaId, TipoAnexoDespesa tipoAnexo);
}
