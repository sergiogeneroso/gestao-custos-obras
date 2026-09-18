package com.seegeneroso.gestao_custos_obras.shared.auditoria.dto;

import com.seegeneroso.gestao_custos_obras.shared.auditoria.OperacaoAuditoria;

import java.time.LocalDateTime;

public record LogAuditoriaResponseDTO(
        Long id,
        String entidade,
        Long entidadeId,
        OperacaoAuditoria operacao,
        Long usuarioId,
        String usuarioNome,
        LocalDateTime dataHora,
        // JSON cru (o ResponseDTO do domínio serializado) — cabe ao consumidor formatar/comparar.
        String estadoAnterior,
        String estadoNovo
) {
}
