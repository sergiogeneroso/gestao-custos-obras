package com.seegeneroso.gestao_custos_obras.despesa;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

// Fragmento do DespesaRepository: soma o valor com o mesmo filtro da listagem paginada.
public interface DespesaRepositoryCustom {

    BigDecimal somarValor(Specification<DespesaModel> especificacao);
}
