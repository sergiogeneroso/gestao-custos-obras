package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.despesa.dto.FiltroDespesaDTO;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoAnexoDespesa;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Consulta da listagem de despesas. Specification em vez de JPQL porque cada filtro só entra quando
 * preenchido: a alternativa "(:param is null or ...)" esbarra no Postgres, que recusa bind nulo sem
 * tipo (ver Buscas).
 */
final class DespesaEspecificacao {

    private DespesaEspecificacao() {
    }

    static Specification<DespesaModel> de(FiltroDespesaDTO filtro) {
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.isTrue(root.get("exclusao").get("ativo")));

            String escopo = filtro.escopo() == null ? "TODAS" : filtro.escopo().toUpperCase();
            if ("IMOVEL".equals(escopo)) {
                condicoes.add(cb.isNotNull(root.get("imovel")));
            } else if ("GERAL".equals(escopo)) {
                condicoes.add(cb.isNull(root.get("imovel")));
            }
            if (filtro.imovelId() != null) {
                condicoes.add(cb.equal(root.get("imovel").get("id"), filtro.imovelId()));
            }
            if (filtro.categoriaDespesaId() != null) {
                condicoes.add(cb.equal(root.get("categoriaDespesa").get("id"), filtro.categoriaDespesaId()));
            }
            if (filtro.pagadorId() != null) {
                condicoes.add(cb.equal(root.get("pagador").get("id"), filtro.pagadorId()));
            }
            if (filtro.beneficiarioId() != null) {
                condicoes.add(cb.equal(root.get("beneficiario").get("id"), filtro.beneficiarioId()));
            }
            if (filtro.faseImovel() != null) {
                condicoes.add(cb.equal(root.get("faseImovel"), filtro.faseImovel()));
            }
            if (filtro.etapaConstrucao() != null) {
                condicoes.add(cb.equal(root.get("etapaConstrucao"), filtro.etapaConstrucao()));
            }
            if (filtro.dataInicio() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(root.get("dataPagamento"), filtro.dataInicio()));
            }
            if (filtro.dataFim() != null) {
                condicoes.add(cb.lessThanOrEqualTo(root.get("dataPagamento"), filtro.dataFim()));
            }
            // Só COMPROVANTE prova o pagamento (ADR-023); recibo e nota fiscal não contam.
            if (Boolean.TRUE.equals(filtro.semComprovante())) {
                Subquery<Long> comprovante = query.subquery(Long.class);
                var anexo = comprovante.from(DespesaAnexoModel.class);
                comprovante.select(anexo.get("id")).where(
                        cb.equal(anexo.get("despesa"), root),
                        cb.equal(anexo.get("tipoAnexo"), TipoAnexoDespesa.COMPROVANTE));
                condicoes.add(cb.not(cb.exists(comprovante)));
            }

            String busca = filtro.busca() == null ? "" : filtro.busca().trim().toLowerCase();
            if (!busca.isEmpty()) {
                // Os mesmos campos que a busca sempre varreu; imóvel e beneficiário são opcionais,
                // daí o left join.
                String termo = "%" + busca + "%";
                condicoes.add(cb.or(
                        cb.like(cb.lower(root.join("categoriaDespesa").get("nome")), termo),
                        cb.like(cb.lower(root.join("pagador").get("nome")), termo),
                        cb.like(cb.lower(cb.coalesce(root.join("beneficiario", JoinType.LEFT).get("nome"), "")), termo),
                        cb.like(cb.lower(cb.coalesce(root.join("imovel", JoinType.LEFT).get("identificador"), "")), termo),
                        cb.like(cb.lower(cb.coalesce(root.get("descricao"), "")), termo)));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }
}
