package com.seegeneroso.gestao_custos_obras.shared.storage;

import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Trava a única regra deste service que entrou no escopo da cobertura de testes financeiros
// (.scratch/cobertura-testes-financeiro/spec.md, decisão 10): nome de arquivo com ".." não pode
// escapar do diretório raiz configurado (path traversal no upload).
class LocalStorageServiceTest {

    @TempDir
    Path diretorioBase;

    @Test
    void recusaNomeDeArquivoComSequenciaDeCaminhoInvalidaSemGravarNada() throws IOException {
        LocalStorageService storageService = new LocalStorageService(diretorioBase.toString());
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "../../malicioso.txt", "text/plain", "conteudo".getBytes());

        assertThatThrownBy(() -> storageService.salvar(arquivo, "imoveis/1"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("sequência de caminho inválida");

        try (var arquivosGravados = Files.walk(diretorioBase)) {
            assertThat(arquivosGravados.filter(Files::isRegularFile)).isEmpty();
        }
    }
}
