package com.seegeneroso.gestao_custos_obras.shared.auditoria;

import com.seegeneroso.gestao_custos_obras.shared.auditoria.dto.LogAuditoriaResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Log de auditoria genérico (ADR-042): toda mutação de um domínio de negócio grava aqui quem fez,
 * quando, e o estado do registro antes/depois. Chamado explicitamente por cada Service — mesmo
 * padrão de ExclusaoLogica/UsuarioAutenticadoService — em vez de captura automática via Envers/AOP
 * (ver .agents/rules/auditoria.md para o porquê).
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void registrar(String entidade, Long entidadeId, OperacaoAuditoria operacao,
                           Object estadoAnterior, Object estadoNovo) {
        LogAuditoriaModel log = LogAuditoriaModel.builder()
                .entidade(entidade)
                .entidadeId(entidadeId)
                .operacao(operacao)
                .usuario(usuarioAutenticadoService.usuarioAtual())
                .dataHora(LocalDateTime.now())
                .estadoAnterior(serializar(estadoAnterior))
                .estadoNovo(serializar(estadoNovo))
                .build();
        logAuditoriaRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<LogAuditoriaResponseDTO> consultar(String entidade, Long entidadeId) {
        return logAuditoriaRepository.findByEntidadeAndEntidadeIdOrderByDataHoraDesc(entidade, entidadeId).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // Jackson 3 (tools.jackson) não tem mais exceção checada aqui, diferente do Jackson 2.
    private String serializar(Object estado) {
        return estado != null ? objectMapper.writeValueAsString(estado) : null;
    }

    private LogAuditoriaResponseDTO toResponseDTO(LogAuditoriaModel log) {
        return new LogAuditoriaResponseDTO(
                log.getId(),
                log.getEntidade(),
                log.getEntidadeId(),
                log.getOperacao(),
                log.getUsuario() != null ? log.getUsuario().getId() : null,
                log.getUsuario() != null ? log.getUsuario().getNome() : null,
                log.getDataHora(),
                log.getEstadoAnterior(),
                log.getEstadoNovo()
        );
    }
}
