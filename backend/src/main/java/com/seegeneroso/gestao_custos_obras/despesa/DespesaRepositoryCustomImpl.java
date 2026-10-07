package com.seegeneroso.gestao_custos_obras.despesa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

@RequiredArgsConstructor
class DespesaRepositoryCustomImpl implements DespesaRepositoryCustom {

    private final EntityManager entityManager;

    // Soma no banco: o total cobre todas as páginas, não só a visível.
    @Override
    public BigDecimal somarValor(Specification<DespesaModel> especificacao) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> consulta = cb.createQuery(BigDecimal.class);
        Root<DespesaModel> root = consulta.from(DespesaModel.class);
        consulta.select(cb.coalesce(cb.sum(root.get("valor")), BigDecimal.ZERO))
                .where(especificacao.toPredicate(root, consulta, cb));
        return entityManager.createQuery(consulta).getSingleResult();
    }
}
