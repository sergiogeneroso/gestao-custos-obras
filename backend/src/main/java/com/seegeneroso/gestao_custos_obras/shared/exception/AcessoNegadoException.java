package com.seegeneroso.gestao_custos_obras.shared.exception;

// Lançada pelo PermissaoInterceptor quando o perfil do usuário autenticado não tem a
// permissão exigida (ADR-046). Mapeada para 403 em ApiErrorHandler.
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String message) {
        super(message);
    }
}
