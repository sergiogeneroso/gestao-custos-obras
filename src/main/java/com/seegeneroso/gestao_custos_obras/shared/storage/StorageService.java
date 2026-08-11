package com.seegeneroso.gestao_custos_obras.shared.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String salvar(MultipartFile arquivo, String subpasta);

    Resource carregarComoRecurso(String nomeArquivo, String subpasta);

    void deletar(String nomeArquivo, String subpasta);
}
