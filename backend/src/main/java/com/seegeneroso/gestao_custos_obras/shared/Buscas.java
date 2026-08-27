package com.seegeneroso.gestao_custos_obras.shared;

/**
 * As consultas paginadas recebem o termo de busca sempre como texto, nunca nulo: vazio vira
 * {@code like '%%'}, que casa com tudo. É o que permite escrever a condição sem
 * {@code :busca is null}, que o Postgres recusa por não conseguir inferir o tipo do bind.
 */
public final class Buscas {

    private Buscas() {
    }

    public static String normalizar(String busca) {
        return busca == null ? "" : busca.trim();
    }
}
