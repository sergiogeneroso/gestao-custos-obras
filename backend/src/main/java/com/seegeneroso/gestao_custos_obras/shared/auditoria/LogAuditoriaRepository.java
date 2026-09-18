package com.seegeneroso.gestao_custos_obras.shared.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoriaModel, Long> {
    List<LogAuditoriaModel> findByEntidadeAndEntidadeIdOrderByDataHoraDesc(String entidade, Long entidadeId);
}
