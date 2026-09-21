package com.seegeneroso.gestao_custos_obras.shared.enums;

// As quatro ações da matriz de permissões do RBAC (ADR-046), derivadas do verbo HTTP pelo
// PermissaoInterceptor — sem anotação por endpoint.
public enum AcaoPermissao {

    ACESSAR, INCLUIR, ALTERAR, DELETAR;

    public static AcaoPermissao porMetodoHttp(String metodoHttp) {
        return switch (metodoHttp) {
            case "GET", "HEAD" -> ACESSAR;
            case "POST" -> INCLUIR;
            case "PUT", "PATCH" -> ALTERAR;
            case "DELETE" -> DELETAR;
            default -> throw new IllegalArgumentException("Método HTTP sem ação de permissão mapeada: " + metodoHttp);
        };
    }
}
