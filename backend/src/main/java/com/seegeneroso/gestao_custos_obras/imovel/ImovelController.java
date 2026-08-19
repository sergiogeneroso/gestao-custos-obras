package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelDocumentoResponseDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFaseRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFotoResponseDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelResponseDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelSituacaoRequestDTO;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoDocumentoImovel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/imoveis")
@RequiredArgsConstructor
public class ImovelController {

    private final ImovelService imovelService;

    @PostMapping
    public ResponseEntity<ImovelResponseDTO> criar(@Valid @RequestBody ImovelRequestDTO dto) {
        ImovelResponseDTO criado = imovelService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<ImovelResponseDTO>> listarTodos() {
        return ResponseEntity.ok(imovelService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImovelResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(imovelService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImovelResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ImovelRequestDTO dto) {
        return ResponseEntity.ok(imovelService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        imovelService.inativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/fase")
    public ResponseEntity<ImovelResponseDTO> avancarFase(@PathVariable Long id, @Valid @RequestBody ImovelFaseRequestDTO dto) {
        return ResponseEntity.ok(imovelService.avancarFase(id, dto));
    }

    @PatchMapping("/{id}/situacao")
    public ResponseEntity<ImovelResponseDTO> alterarSituacao(@PathVariable Long id, @Valid @RequestBody ImovelSituacaoRequestDTO dto) {
        return ResponseEntity.ok(imovelService.alterarSituacao(id, dto));
    }

    @PostMapping(value = "/{id}/fotos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImovelFotoResponseDTO> adicionarFoto(
            @PathVariable Long id,
            @RequestParam("arquivo") org.springframework.web.multipart.MultipartFile arquivo,
            @RequestParam(value = "legenda", required = false) String legenda) {
        ImovelFotoResponseDTO foto = imovelService.adicionarFoto(id, arquivo, legenda);
        return ResponseEntity.status(HttpStatus.CREATED).body(foto);
    }

    @GetMapping("/{id}/fotos")
    public ResponseEntity<List<ImovelFotoResponseDTO>> listarFotos(@PathVariable Long id) {
        return ResponseEntity.ok(imovelService.listarFotos(id));
    }

    @PatchMapping("/{id}/fotos/{fotoId}/principal")
    public ResponseEntity<List<ImovelFotoResponseDTO>> definirFotoPrincipal(@PathVariable Long id, @PathVariable Long fotoId) {
        return ResponseEntity.ok(imovelService.definirFotoPrincipal(id, fotoId));
    }

    @DeleteMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<Void> deletarFoto(@PathVariable Long id, @PathVariable Long fotoId) {
        imovelService.deletarFoto(id, fotoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/documentos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImovelDocumentoResponseDTO> adicionarDocumento(
            @PathVariable Long id,
            @RequestParam("arquivo") org.springframework.web.multipart.MultipartFile arquivo,
            @RequestParam("tipoDocumento") TipoDocumentoImovel tipoDocumento,
            @RequestParam(value = "faseImovel", required = false) FaseImovel faseImovel,
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "dataEmissao", required = false) java.time.LocalDate dataEmissao,
            @RequestParam(value = "dataValidade", required = false) java.time.LocalDate dataValidade) {
        ImovelDocumentoResponseDTO documento = imovelService.adicionarDocumento(
                id, arquivo, tipoDocumento, faseImovel, descricao, dataEmissao, dataValidade);
        return ResponseEntity.status(HttpStatus.CREATED).body(documento);
    }

    @GetMapping("/{id}/documentos")
    public ResponseEntity<List<ImovelDocumentoResponseDTO>> listarDocumentos(
            @PathVariable Long id,
            @RequestParam(required = false) TipoDocumentoImovel tipoDocumento) {
        return ResponseEntity.ok(imovelService.listarDocumentos(id, tipoDocumento));
    }

    @DeleteMapping("/{id}/documentos/{documentoId}")
    public ResponseEntity<Void> deletarDocumento(@PathVariable Long id, @PathVariable Long documentoId) {
        imovelService.deletarDocumento(id, documentoId);
        return ResponseEntity.noContent().build();
    }
}
