package com.seegeneroso.gestao_custos_obras.shared.storage;

import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path diretorioRaiz;

    public LocalStorageService(@Value("${app.upload.dir:uploads}") String diretorioUpload) {
        this.diretorioRaiz = Paths.get(diretorioUpload).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.diretorioRaiz);
        } catch (IOException e) {
            throw new RegraDeNegocioException("Não foi possível criar o diretório de armazenamento de arquivos: " + e.getMessage());
        }
    }

    @Override
    public String salvar(MultipartFile arquivo, String subpasta) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraDeNegocioException("O arquivo enviado não pode estar vazio.");
        }

        String nomeOriginal = StringUtils.cleanPath(Objects.requireNonNull(arquivo.getOriginalFilename()));
        if (nomeOriginal.contains("..")) {
            throw new RegraDeNegocioException("O nome do arquivo contém sequência de caminho inválida: " + nomeOriginal);
        }

        String nomeArquivo = UUID.randomUUID() + "_" + nomeOriginal;
        Path diretorioDestino = this.diretorioRaiz.resolve(subpasta).normalize();

        try {
            Files.createDirectories(diretorioDestino);
            Path caminhoCompleto = diretorioDestino.resolve(nomeArquivo);
            Files.copy(arquivo.getInputStream(), caminhoCompleto, StandardCopyOption.REPLACE_EXISTING);
            return nomeArquivo;
        } catch (IOException e) {
            throw new RegraDeNegocioException("Falha ao salvar o arquivo: " + e.getMessage());
        }
    }

    @Override
    public Resource carregarComoRecurso(String nomeArquivo, String subpasta) {
        try {
            Path caminhoArquivo = this.diretorioRaiz.resolve(subpasta).resolve(nomeArquivo).normalize();
            Resource recurso = new UrlResource(caminhoArquivo.toUri());

            if (recurso.exists() && recurso.isReadable()) {
                return recurso;
            } else {
                throw new RecursoNaoEncontradoException("Arquivo não encontrado: " + nomeArquivo);
            }
        } catch (MalformedURLException e) {
            throw new RecursoNaoEncontradoException("Arquivo não encontrado: " + nomeArquivo);
        }
    }

    @Override
    public void deletar(String nomeArquivo, String subpasta) {
        try {
            Path caminhoArquivo = this.diretorioRaiz.resolve(subpasta).resolve(nomeArquivo).normalize();
            Files.deleteIfExists(caminhoArquivo);
        } catch (IOException e) {
            throw new RegraDeNegocioException("Falha ao remover o arquivo: " + e.getMessage());
        }
    }
}
