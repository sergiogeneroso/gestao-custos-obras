package com.seegeneroso.gestao_custos_obras.shared.validacao;

/**
 * Validação e normalização de CPF/CNPJ.
 *
 * O documento é gravado sempre normalizado (só os caracteres significativos, sem
 * pontuação): é isso que faz a checagem de duplicidade de PessoaService funcionar —
 * comparando a string bruta, o mesmo CPF com e sem pontos entrava como duas pessoas.
 * A pontuação é responsabilidade da tela (ver shared/mascara/ no frontend).
 */
public final class Documentos {

    private Documentos() {
    }

    /** Remove tudo que não seja letra ou dígito e sobe para maiúsculas. */
    public static String normalizar(String documento) {
        return documento == null ? null : documento.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    public static String apenasDigitos(String texto) {
        return texto == null ? null : texto.replaceAll("[^0-9]", "");
    }

    public static boolean cpfValido(String documento) {
        String cpf = normalizar(documento);
        if (cpf == null || cpf.length() != 11 || !cpf.chars().allMatch(Character::isDigit) || todosIguais(cpf)) {
            return false;
        }
        int[] pesosPrimeiro = {10, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesosSegundo = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
        return digitoCpf(cpf, pesosPrimeiro) == cpf.charAt(9) - '0'
                && digitoCpf(cpf, pesosSegundo) == cpf.charAt(10) - '0';
    }

    /**
     * Aceita o CNPJ numérico de sempre e também o alfanumérico que passou a ser emitido
     * em 2026. O algoritmo é o mesmo: cada caractere vale `ASCII - 48`, o que devolve o
     * próprio dígito quando ele é numérico. Os dois verificadores continuam numéricos.
     */
    public static boolean cnpjValido(String documento) {
        String cnpj = normalizar(documento);
        if (cnpj == null || cnpj.length() != 14 || todosIguais(cnpj)) {
            return false;
        }
        if (!cnpj.chars().allMatch(c -> Character.isDigit(c) || (c >= 'A' && c <= 'Z'))) {
            return false;
        }
        if (!Character.isDigit(cnpj.charAt(12)) || !Character.isDigit(cnpj.charAt(13))) {
            return false;
        }
        int[] pesosPrimeiro = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesosSegundo = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return digitoCnpj(cnpj, pesosPrimeiro) == cnpj.charAt(12) - '0'
                && digitoCnpj(cnpj, pesosSegundo) == cnpj.charAt(13) - '0';
    }

    private static int digitoCpf(String cpf, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cpf.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static int digitoCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - 48) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    // 111.111.111-11 passa em todos os verificadores e não é documento de ninguém.
    private static boolean todosIguais(String valor) {
        return valor.chars().distinct().count() == 1;
    }
}
