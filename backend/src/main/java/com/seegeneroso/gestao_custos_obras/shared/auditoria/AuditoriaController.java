package com.seegeneroso.gestao_custos_obras.shared.auditoria;

import com.seegeneroso.gestao_custos_obras.shared.auditoria.dto.LogAuditoriaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoint único e compartilhado (ver .agents/rules/auditoria.md): toda tela "ver histórico" de
// qualquer domínio consulta aqui, em vez de cada domínio ganhar sua própria rota /historico.
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<List<LogAuditoriaResponseDTO>> consultar(
            @RequestParam String entidade,
            @RequestParam Long entidadeId) {
        return ResponseEntity.ok(auditoriaService.consultar(entidade, entidadeId));
    }
}
