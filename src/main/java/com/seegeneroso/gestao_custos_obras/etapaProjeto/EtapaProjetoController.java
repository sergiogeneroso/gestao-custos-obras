package com.seegeneroso.gestao_custos_obras.etapaProjeto;

import com.seegeneroso.gestao_custos_obras.etapaProjeto.dto.EtapaProjetoRequestDTO;
import com.seegeneroso.gestao_custos_obras.etapaProjeto.dto.EtapaProjetoResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/etapas-projeto")
@RequiredArgsConstructor
public class EtapaProjetoController {

    private final EtapaProjetoService etapaProjetoService;

    @PostMapping
    public ResponseEntity<EtapaProjetoResponseDTO> criar(@Valid @RequestBody EtapaProjetoRequestDTO dto) {
        EtapaProjetoResponseDTO criada = etapaProjetoService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(uri).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<EtapaProjetoResponseDTO>> listarTodas() {
        return ResponseEntity.ok(etapaProjetoService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EtapaProjetoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(etapaProjetoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EtapaProjetoResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody EtapaProjetoRequestDTO dto) {
        return ResponseEntity.ok(etapaProjetoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        etapaProjetoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
