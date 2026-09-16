package com.seegeneroso.gestao_custos_obras.pessoa;

import com.seegeneroso.gestao_custos_obras.pessoa.dto.PessoaRequestDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.dto.PessoaResponseDTO;
import jakarta.validation.Valid;
import com.seegeneroso.gestao_custos_obras.shared.PaginaDTO;
import com.seegeneroso.gestao_custos_obras.shared.exclusao.ExclusaoRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pessoas")
@RequiredArgsConstructor
public class PessoaController {

    private final PessoaService pessoaService;

    @PostMapping
    public ResponseEntity<PessoaResponseDTO> criar(@Valid @RequestBody PessoaRequestDTO dto) {
        PessoaResponseDTO criado = pessoaService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<PessoaResponseDTO>> listarTodos() {
        return ResponseEntity.ok(pessoaService.listarTodos());
    }

    // Endpoint da tela de listagem: busca e filtros vão para o banco junto com a paginação.
    // O GET sem /pagina continua devolvendo a lista inteira, que é o que alimenta os combos.
    @GetMapping("/pagina")
    public ResponseEntity<PaginaDTO<PessoaResponseDTO>> buscar(
            @RequestParam(defaultValue = "") String busca,
            @RequestParam(defaultValue = "false") boolean somenteFornecedores,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return ResponseEntity.ok(pessoaService.buscar(busca, somenteFornecedores, pagina, tamanho));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PessoaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pessoaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PessoaResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody PessoaRequestDTO dto) {
        return ResponseEntity.ok(pessoaService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @Valid @RequestBody ExclusaoRequestDTO dto) {
        pessoaService.excluir(id, dto.motivo());
        return ResponseEntity.noContent().build();
    }
}
