package com.seegeneroso.gestao_custos_obras.categoriaDespesa;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.dto.CategoriaDespesaRequestDTO;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.dto.CategoriaDespesaResponseDTO;
import jakarta.validation.Valid;
import com.seegeneroso.gestao_custos_obras.shared.PaginaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categorias-despesa")
@RequiredArgsConstructor
public class CategoriaDespesaController {

    private final CategoriaDespesaService categoriaDespesaService;

    @PostMapping
    public ResponseEntity<CategoriaDespesaResponseDTO> criar(@Valid @RequestBody CategoriaDespesaRequestDTO dto) {
        CategoriaDespesaResponseDTO criada = categoriaDespesaService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(uri).body(criada);
    }

    @GetMapping
    public ResponseEntity<List<CategoriaDespesaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(categoriaDespesaService.listarTodas());
    }

    // Endpoint da tela de listagem: busca e filtros vão para o banco junto com a paginação.
    // O GET sem /pagina continua devolvendo a lista inteira, que é o que alimenta os combos.
    @GetMapping("/pagina")
    public ResponseEntity<PaginaDTO<CategoriaDespesaResponseDTO>> buscar(
            @RequestParam(defaultValue = "") String busca,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(categoriaDespesaService.buscar(busca, pagina, tamanho));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDespesaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaDespesaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDespesaResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaDespesaRequestDTO dto) {
        return ResponseEntity.ok(categoriaDespesaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        categoriaDespesaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
