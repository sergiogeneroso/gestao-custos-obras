package com.seegeneroso.gestao_custos_obras.shared.storage;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * A URL do anexo é gravada **relativa à raiz** ({@code /api/arquivos/download/<subpasta>/<nome>}).
 *
 * <p>Antes disso, os quatro pontos de upload gravavam a URL absoluta montada por
 * {@code ServletUriComponentsBuilder.fromCurrentContextPath()} — ou seja, o host que atendeu
 * aquele upload: {@code localhost:4200} quando veio pela tela via proxy, {@code localhost:8080}
 * quando veio direto no backend. Trocar de host ou de porta quebrava todas as URLs já gravadas.
 * Como o frontend consome tudo por {@code HttpClient} e o {@code apiUrl} dele já é {@code /api},
 * a URL relativa resolve sozinha contra a origem de quem estiver servindo a aplicação.
 *
 * <p>Os leitores toleram o formato antigo: {@link #subpastaDe} e {@link #nomeArquivoDe} descartam
 * o {@code http://host} do começo, então linha gravada antes da mudança continua sendo encontrada
 * e apagada.
 */
public final class ArquivoUrls {

    private static final String PREFIXO = "/api/arquivos/download/";

    private ArquivoUrls() {
    }

    // O nome do arquivo preserva o nome original enviado pelo usuário, que pode ter espaço e
    // acento, então o segmento vai percent-encoded — como o ServletUriComponentsBuilder já fazia.
    // Isso mantém o formato idêntico ao das linhas antigas: só o host sai da frente.
    public static String montar(String subpasta, String nomeArquivo) {
        return PREFIXO + subpasta + "/" + UriUtils.encodePathSegment(nomeArquivo, StandardCharsets.UTF_8);
    }

    /** Subpasta de armazenamento (ex. {@code imoveis/5/documentos}), ou vazio se a URL não tiver uma. */
    public static String subpastaDe(String url) {
        String caminho = caminhoRelativo(url);
        int ultimaBarra = caminho.lastIndexOf('/');
        return ultimaBarra >= 0 ? decodificar(caminho.substring(0, ultimaBarra)) : "";
    }

    /** Nome como está no disco — decodificado, porque é assim que o {@link StorageService} o encontra. */
    public static String nomeArquivoDe(String url) {
        String caminho = caminhoRelativo(url);
        int ultimaBarra = caminho.lastIndexOf('/');
        return decodificar(ultimaBarra >= 0 ? caminho.substring(ultimaBarra + 1) : caminho);
    }

    private static String decodificar(String segmento) {
        try {
            return UriUtils.decode(segmento, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // URL gravada fora do padrão (um '%' solto, por exemplo): devolve como está em vez de
            // derrubar a remoção do registro.
            return segmento;
        }
    }

    private static String caminhoRelativo(String url) {
        if (url == null) {
            return "";
        }
        int inicio = url.indexOf(PREFIXO);
        return inicio >= 0 ? url.substring(inicio + PREFIXO.length()) : "";
    }
}
