package com.seegeneroso.gestao_custos_obras.shared.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// A ida e a volta precisam fechar: se subpastaDe/nomeArquivoDe divergirem do que montar() gravou,
// remover um anexo apaga o registro e deixa o arquivo no disco — a dívida que esta classe fecha.
class ArquivoUrlsTest {

    @Test
    void montaUrlRelativaSemHost() {
        assertThat(ArquivoUrls.montar("imoveis/5", "abc_foto.jpg"))
                .isEqualTo("/api/arquivos/download/imoveis/5/abc_foto.jpg");
    }

    @Test
    void idaEVoltaPreservaSubpastaAninhadaENomeDoArquivo() {
        String url = ArquivoUrls.montar("imoveis/5/documentos", "abc_matricula.pdf");

        assertThat(ArquivoUrls.subpastaDe(url)).isEqualTo("imoveis/5/documentos");
        assertThat(ArquivoUrls.nomeArquivoDe(url)).isEqualTo("abc_matricula.pdf");
    }

    // Linha gravada antes da mudança guardava o host de quem atendeu o upload — e já vinha
    // percent-encoded, então a decodificação também vale para ela.
    @Test
    void urlAbsolutaAntigaContinuaSendoLida() {
        String antiga = "http://localhost:4200/api/arquivos/download/despesas/7/abc_nota%20fiscal.pdf";

        assertThat(ArquivoUrls.subpastaDe(antiga)).isEqualTo("despesas/7");
        assertThat(ArquivoUrls.nomeArquivoDe(antiga)).isEqualTo("abc_nota fiscal.pdf");
    }

    // O nome no disco preserva o original enviado pelo usuário, que pode ter espaço e acento: na
    // URL ele vai codificado, mas quem apaga o arquivo precisa do nome cru de volta.
    @Test
    void nomeComEspacoEAcentoVaiCodificadoEVoltaCru() {
        String url = ArquivoUrls.montar("imoveis/5", "abc_nota fiscal ção.pdf");

        assertThat(url).doesNotContain(" ").contains("%20");
        assertThat(ArquivoUrls.nomeArquivoDe(url)).isEqualTo("abc_nota fiscal ção.pdf");
        assertThat(ArquivoUrls.subpastaDe(url)).isEqualTo("imoveis/5");
    }

    // URL fora do padrão não pode derrubar a remoção do registro: devolve vazio e o storage não
    // encontra nada para apagar.
    @Test
    void urlNulaOuForaDoPadraoNaoQuebra() {
        assertThat(ArquivoUrls.subpastaDe(null)).isEmpty();
        assertThat(ArquivoUrls.nomeArquivoDe(null)).isEmpty();
        assertThat(ArquivoUrls.nomeArquivoDe("https://cdn.exemplo.com/foto.jpg")).isEmpty();
    }
}
