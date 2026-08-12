package com.seegeneroso.gestao_custos_obras.aportante;

import com.seegeneroso.gestao_custos_obras.aportante.dto.AportanteRequestDTO;
import com.seegeneroso.gestao_custos_obras.aportante.dto.AportanteResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/aportantes")
@RequiredArgsConstructor
public class AportanteController {

    private final AportanteService aportanteService;

    @PostMapping
    public ResponseEntity<AportanteResponseDTO> criar(@Valid @RequestBody AportanteRequestDTO dto) {
        AportanteResponseDTO criado = aportanteService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<AportanteResponseDTO>> listarTodos() {
        return ResponseEntity.ok(aportanteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AportanteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(aportanteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AportanteResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody AportanteRequestDTO dto) {
        return ResponseEntity.ok(aportanteService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        aportanteService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
