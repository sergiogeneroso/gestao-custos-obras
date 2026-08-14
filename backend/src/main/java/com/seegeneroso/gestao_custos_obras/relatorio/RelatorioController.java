package com.seegeneroso.gestao_custos_obras.relatorio;

import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorImovelDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorM2DTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.ExtratoPessoaDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.OrcadoVsRealizadoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;
    private static final String CSV_MEDIA = "text/csv; charset=UTF-8";

    @GetMapping("/custo-por-imovel")
    public ResponseEntity<?> custoPorImovel(
            @RequestParam(required = false) Long imovelId,
            @RequestParam(required = false) Long categoriaDespesaId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String format) {
        List<CustoPorImovelDTO> dados = relatorioService.custoPorImovel(imovelId, categoriaDespesaId, dataInicio, dataFim);
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder("imovelId;identificador;custoTotal\n");
            dados.forEach(d -> sb.append(d.imovelId()).append(';').append(d.identificador()).append(';').append(d.custoTotal()).append('\n'));
            return csvResponse(sb, "custo-por-imovel.csv");
        }
        return ResponseEntity.ok(dados);
    }

    @GetMapping("/custo-por-m2")
    public ResponseEntity<?> custoPorM2(
            @RequestParam(required = false) Long imovelId,
            @RequestParam(required = false) Long categoriaDespesaId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String format) {
        CustoPorM2DTO dado = relatorioService.custoPorM2(imovelId, categoriaDespesaId, dataInicio, dataFim);
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder("imovelId;identificador;area;custoTotal;custoPorM2\n");
            sb.append(dado.imovelId()).append(';').append(dado.identificador()).append(';')
                    .append(dado.area()).append(';').append(dado.custoTotal()).append(';').append(dado.custoPorM2()).append('\n');
            return csvResponse(sb, "custo-por-m2.csv");
        }
        return ResponseEntity.ok(dado);
    }

    @GetMapping("/extrato-pessoas")
    public ResponseEntity<?> extratoPessoas(
            @RequestParam(required = false) Long imovelId,
            @RequestParam(required = false) Long categoriaDespesaId,
            @RequestParam(required = false) Long pessoaId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String format) {
        List<ExtratoPessoaDTO> dados = relatorioService.extratoPessoas(imovelId, categoriaDespesaId, pessoaId, dataInicio, dataFim);
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder("pessoaId;nome;totalPago\n");
            dados.forEach(d -> sb.append(d.pessoaId()).append(';').append(d.nome()).append(';').append(d.totalPago()).append('\n'));
            return csvResponse(sb, "extrato-pessoas.csv");
        }
        return ResponseEntity.ok(dados);
    }

    @GetMapping("/orcado-vs-realizado")
    public ResponseEntity<?> orcadoVsRealizado(
            @RequestParam(required = false) Long imovelId,
            @RequestParam(required = false) String format) {
        OrcadoVsRealizadoDTO dado = relatorioService.orcadoVsRealizado(imovelId);
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder("imovelId;identificador;valorOrcadoTotal;valorRealizadoTotal;diferenca\n");
            sb.append(dado.imovelId()).append(';').append(dado.identificador()).append(';')
                    .append(dado.valorOrcadoTotal()).append(';').append(dado.valorRealizadoTotal()).append(';')
                    .append(dado.diferenca()).append('\n');
            return csvResponse(sb, "orcado-vs-realizado.csv");
        }
        return ResponseEntity.ok(dado);
    }

    private ResponseEntity<String> csvResponse(StringBuilder sb, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(CSV_MEDIA))
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(sb.toString());
    }
}
