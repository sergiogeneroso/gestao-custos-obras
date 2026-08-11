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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/arquivos/download/")
                .path(subpasta + "/")
                .path(nomeArquivo)
                .toUriString();

        ArquivoResponseDTO response = new ArquivoResponseDTO(
                nomeArquivo,
                fileDownloadUri,
                arquivo.getContentType(),
                arquivo.getSize()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{subpasta}/{nomeArquivo:.+}")
    public ResponseEntity<Resource> download(
            @PathVariable String subpasta,
            @PathVariable String nomeArquivo,
            HttpServletRequest request) {

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
