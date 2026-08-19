package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoDocumentoResponseDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroResponseDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoQuitacaoRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ParcelaPagamentoRequestDTO;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoDocumentoContrato;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/contratos-financeiros")
@RequiredArgsConstructor
public class ContratoFinanceiroController {

    private final ContratoFinanceiroService contratoFinanceiroService;

    @PostMapping
    public ResponseEntity<ContratoFinanceiroResponseDTO> criar(@Valid @RequestBody ContratoFinanceiroRequestDTO dto) {
        ContratoFinanceiroResponseDTO criado = contratoFinanceiroService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @GetMapping
    public ResponseEntity<List<ContratoFinanceiroResponseDTO>> listar(@RequestParam(required = false) Long imovelId) {
        return ResponseEntity.ok(contratoFinanceiroService.listar(imovelId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoFinanceiroResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contratoFinanceiroService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContratoFinanceiroResponseDTO> atualizar(
            @PathVariable Long id, @Valid @RequestBody ContratoFinanceiroRequestDTO dto) {
        return ResponseEntity.ok(contratoFinanceiroService.atualizar(id, dto));
    }

    @PatchMapping("/{id}/quitar")
    public ResponseEntity<ContratoFinanceiroResponseDTO> quitar(@PathVariable Long id, @Valid @RequestBody ContratoQuitacaoRequestDTO dto) {
        return ResponseEntity.ok(contratoFinanceiroService.quitar(id, dto));
    }

    @PatchMapping("/{id}/parcelas/{parcelaId}/pagamento")
    public ResponseEntity<ContratoFinanceiroResponseDTO> pagarParcela(
            @PathVariable Long id, @PathVariable Long parcelaId, @Valid @RequestBody ParcelaPagamentoRequestDTO dto) {
        return ResponseEntity.ok(contratoFinanceiroService.pagarParcela(id, parcelaId, dto));
    }
    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContratoDocumentoResponseDTO> adicionarDocumento(
            @PathVariable Long id,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam TipoDocumentoContrato tipoDocumento,
            @RequestParam(required = false) String descricao) {
        return ResponseEntity.ok(contratoFinanceiroService.adicionarDocumento(id, arquivo, tipoDocumento, descricao));
    }

    @GetMapping("/{id}/documentos")
    public ResponseEntity<List<ContratoDocumentoResponseDTO>> listarDocumentos(@PathVariable Long id) {
        return ResponseEntity.ok(contratoFinanceiroService.listarDocumentos(id));
    }

    @DeleteMapping("/{id}/documentos/{documentoId}")
    public ResponseEntity<Void> deletarDocumento(@PathVariable Long id, @PathVariable Long documentoId) {
        contratoFinanceiroService.deletarDocumento(id, documentoId);
        return ResponseEntity.noContent().build();
    }

}
