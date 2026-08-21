package com.seegeneroso.gestao_custos_obras.shared.storage;

import com.seegeneroso.gestao_custos_obras.shared.storage.dto.ArquivoResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/arquivos")
@RequiredArgsConstructor
public class ArquivoController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArquivoResponseDTO> upload(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "subpasta", defaultValue = "geral") String subpasta) {

        String nomeArquivo = storageService.salvar(arquivo, subpasta);
        String fileDownloadUri = ArquivoUrls.montar(subpasta, nomeArquivo);

        ArquivoResponseDTO response = new ArquivoResponseDTO(
                nomeArquivo,
                fileDownloadUri,
                arquivo.getContentType(),
                arquivo.getSize()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{*caminho}")
    public ResponseEntity<Resource> download(@PathVariable String caminho, HttpServletRequest request) {
        // ponytail: PathPatternParser não permite {subpasta} cruzar barras (subpasta
        // pode ser "imoveis/{id}") — captura o caminho inteiro e separa manualmente
        String caminhoLimpo = caminho.startsWith("/") ? caminho.substring(1) : caminho;
        int ultimaBarra = caminhoLimpo.lastIndexOf('/');
        String subpasta = ultimaBarra >= 0 ? caminhoLimpo.substring(0, ultimaBarra) : "";
        String nomeArquivo = ultimaBarra >= 0 ? caminhoLimpo.substring(ultimaBarra + 1) : caminhoLimpo;

        Resource recurso = storageService.carregarComoRecurso(nomeArquivo, subpasta);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(recurso.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // fallback
        }

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recurso.getFilename() + "\"")
                .body(recurso);
    }
}
