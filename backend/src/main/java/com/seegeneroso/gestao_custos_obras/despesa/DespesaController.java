package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaAnexoResponseDTO;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaRequestDTO;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoAnexoDespesa;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @RequestParam(required = false) Long categoriaDespesaId,
            @RequestParam(required = false) Boolean semImovel) {
        return ResponseEntity.ok(despesaService.listar(imovelId, categoriaDespesaId, semImovel));
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
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        despesaService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/anexos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DespesaAnexoResponseDTO> adicionarAnexo(
            @PathVariable Long id,
            @RequestParam("arquivo") org.springframework.web.multipart.MultipartFile arquivo,
            @RequestParam("tipoAnexo") TipoAnexoDespesa tipoAnexo) {
        DespesaAnexoResponseDTO anexo = despesaService.adicionarAnexo(id, arquivo, tipoAnexo);
        return ResponseEntity.status(HttpStatus.CREATED).body(anexo);
    }

    @GetMapping("/{id}/anexos")
    public ResponseEntity<List<DespesaAnexoResponseDTO>> listarAnexos(
            @PathVariable Long id,
            @RequestParam(required = false) TipoAnexoDespesa tipoAnexo) {
        return ResponseEntity.ok(despesaService.listarAnexos(id, tipoAnexo));
    }

    @DeleteMapping("/{id}/anexos/{anexoId}")
    public ResponseEntity<Void> deletarAnexo(@PathVariable Long id, @PathVariable Long anexoId) {
        despesaService.deletarAnexo(id, anexoId);
        return ResponseEntity.noContent().build();
    }
}
