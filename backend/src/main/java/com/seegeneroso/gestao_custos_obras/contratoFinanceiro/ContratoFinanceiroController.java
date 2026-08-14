package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroResponseDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoQuitacaoRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ParcelaPagamentoRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PatchMapping("/{id}/quitar")
    public ResponseEntity<ContratoFinanceiroResponseDTO> quitar(@PathVariable Long id, @Valid @RequestBody ContratoQuitacaoRequestDTO dto) {
        return ResponseEntity.ok(contratoFinanceiroService.quitar(id, dto));
    }

    @PatchMapping("/{id}/parcelas/{parcelaId}/pagamento")
    public ResponseEntity<ContratoFinanceiroResponseDTO> pagarParcela(
            @PathVariable Long id, @PathVariable Long parcelaId, @Valid @RequestBody ParcelaPagamentoRequestDTO dto) {
        return ResponseEntity.ok(contratoFinanceiroService.pagarParcela(id, parcelaId, dto));
    }
}
