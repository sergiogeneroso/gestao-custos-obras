package com.seegeneroso.gestao_custos_obras.shared.enums;

import java.util.Arrays;
import java.util.Optional;

// Unidade de autorização do RBAC (ADR-046): um por pacote de negócio (package-by-feature).
// Lista fixa no código — só a matriz perfil×domínio×ação é editável em runtime, não os domínios.
public enum DominioSistema {

    IMOVEL("imoveis"),
    PESSOA("pessoas"),
    DESPESA("despesas"),
    CONTRATO_FINANCEIRO("contratos-financeiros"),
    CATEGORIA_DESPESA("categorias-despesa"),
    RELATORIO("relatorios"),
    AUDITORIA("auditoria"),
    PERFIL("perfis");

    private final String prefixoUrl;

    DominioSistema(String prefixoUrl) {
        this.prefixoUrl = prefixoUrl;
    }

    public static Optional<DominioSistema> porPrefixoUrl(String prefixo) {
        return Arrays.stream(values()).filter(d -> d.prefixoUrl.equals(prefixo)).findFirst();
    }
}
