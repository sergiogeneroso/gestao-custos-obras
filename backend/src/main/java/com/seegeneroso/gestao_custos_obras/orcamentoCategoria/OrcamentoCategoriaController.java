package com.seegeneroso.gestao_custos_obras.orcamentoCategoria;

import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto.OrcamentoCategoriaRequestDTO;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto.OrcamentoCategoriaResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orcamentos-categoria")
@RequiredArgsConstructor
public class OrcamentoCategoriaController {

    private final OrcamentoCategoriaService orcamentoCategoriaService;

    @PostMapping
    public ResponseEntity<OrcamentoCategoriaResponseDTO> criar(@Valid @RequestBody OrcamentoCategoriaRequestDTO dto) {
        OrcamentoCategoriaResponseDTO criado = orcamentoCategoriaService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<OrcamentoCategoriaResponseDTO>> listar(
            @RequestParam(required = false) Long imovelId) {
        return ResponseEntity.ok(orcamentoCategoriaService.listar(imovelId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrcamentoCategoriaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(orcamentoCategoriaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrcamentoCategoriaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OrcamentoCategoriaRequestDTO dto) {
        return ResponseEntity.ok(orcamentoCategoriaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        orcamentoCategoriaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
