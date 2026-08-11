package com.seegeneroso.gestao_custos_obras.orcamentoEtapa;

import com.seegeneroso.gestao_custos_obras.orcamentoEtapa.dto.OrcamentoEtapaRequestDTO;
import com.seegeneroso.gestao_custos_obras.orcamentoEtapa.dto.OrcamentoEtapaResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orcamentos-etapa")
@RequiredArgsConstructor
public class OrcamentoEtapaController {

    private final OrcamentoEtapaService orcamentoEtapaService;

    @PostMapping
    public ResponseEntity<OrcamentoEtapaResponseDTO> criar(@Valid @RequestBody OrcamentoEtapaRequestDTO dto) {
        OrcamentoEtapaResponseDTO criado = orcamentoEtapaService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<OrcamentoEtapaResponseDTO>> listar(
            @RequestParam(required = false) Long imovelId) {
        return ResponseEntity.ok(orcamentoEtapaService.listar(imovelId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrcamentoEtapaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(orcamentoEtapaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrcamentoEtapaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OrcamentoEtapaRequestDTO dto) {
        return ResponseEntity.ok(orcamentoEtapaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        orcamentoEtapaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
