package com.seegeneroso.gestao_custos_obras.shared;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados devolvida pelas listagens.
 *
 * Existe em vez de serializar o `Page` do Spring Data direto porque o formato do `PageImpl` não é
 * contrato estável de API — e porque os nomes aqui seguem o português do resto do projeto.
 */
public record PaginaDTO<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {

    public static <E, T> PaginaDTO<T> de(Page<E> page, Function<E, T> paraDTO) {
        return new PaginaDTO<>(
                page.getContent().stream().map(paraDTO).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
