package com.seegeneroso.gestao_custos_obras.perfil;

import com.seegeneroso.gestao_custos_obras.perfil.dto.MinhasPermissoesResponseDTO;
import com.seegeneroso.gestao_custos_obras.perfil.dto.PerfilRequestDTO;
import com.seegeneroso.gestao_custos_obras.perfil.dto.PerfilResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.exclusao.ExclusaoRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/perfis")
@RequiredArgsConstructor
public class PerfilController {

    private final PerfilService perfilService;

    @PostMapping
    public ResponseEntity<PerfilResponseDTO> criar(@Valid @RequestBody PerfilRequestDTO dto) {
        PerfilResponseDTO criado = perfilService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<PerfilResponseDTO>> listarTodos() {
        return ResponseEntity.ok(perfilService.listarTodos());
    }

    // Qualquer usuário autenticado consulta as PRÓPRIAS permissões, independente de ter acesso
    // ao domínio "perfil" — é o que alimenta a UI (menu/botões) de quem loga (ADR-046). Por isso
    // o PermissaoInterceptor exclui esta rota específica da checagem geral do domínio "perfil".
    @GetMapping("/minhas-permissoes")
    public ResponseEntity<MinhasPermissoesResponseDTO> minhasPermissoes() {
        return ResponseEntity.ok(perfilService.minhasPermissoes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerfilResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(perfilService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PerfilResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody PerfilRequestDTO dto) {
        return ResponseEntity.ok(perfilService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @Valid @RequestBody ExclusaoRequestDTO dto) {
        perfilService.excluir(id, dto.motivo());
        return ResponseEntity.noContent().build();
    }
}
