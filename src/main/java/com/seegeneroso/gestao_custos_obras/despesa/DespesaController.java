package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaRequestDTO;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/despesas")
@RequiredArgsConstructor
public class DespesaController {

    private final DespesaService despesaService;

    @PostMapping
    public ResponseEntity<DespesaResponseDTO> criar(@Valid @RequestBody DespesaRequestDTO dto) {
        DespesaResponseDTO criada = despesaService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(uri).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<DespesaResponseDTO>> listar(
            @RequestParam(required = false) Long imovelId,
            @RequestParam(required = false) Long etapaProjetoId) {
        return ResponseEntity.ok(despesaService.listar(imovelId, etapaProjetoId));
    }

    @GetMapping("/imovel/{imovelId}")
    public ResponseEntity<List<DespesaResponseDTO>> listarPorImovel(@PathVariable Long imovelId) {
        return ResponseEntity.ok(despesaService.listarPorImovel(imovelId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DespesaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(despesaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DespesaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody DespesaRequestDTO dto) {
        return ResponseEntity.ok(despesaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        despesaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
