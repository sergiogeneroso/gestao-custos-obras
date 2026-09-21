package com.seegeneroso.gestao_custos_obras.shared.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Cobre o mapeamento verbo HTTP -> ação da matriz de permissões que o PermissaoInterceptor usa
// pra dispensar anotação por endpoint (ADR-046).
class AcaoPermissaoTest {

    @Test
    void getEHeadMapeiamParaAcessar() {
        assertThat(AcaoPermissao.porMetodoHttp("GET")).isEqualTo(AcaoPermissao.ACESSAR);
        assertThat(AcaoPermissao.porMetodoHttp("HEAD")).isEqualTo(AcaoPermissao.ACESSAR);
    }

    @Test
    void postMapeiaParaIncluir() {
        assertThat(AcaoPermissao.porMetodoHttp("POST")).isEqualTo(AcaoPermissao.INCLUIR);
    }

    @Test
    void putEPatchMapeiamParaAlterar() {
        assertThat(AcaoPermissao.porMetodoHttp("PUT")).isEqualTo(AcaoPermissao.ALTERAR);
        assertThat(AcaoPermissao.porMetodoHttp("PATCH")).isEqualTo(AcaoPermissao.ALTERAR);
    }

    @Test
    void deleteMapeiaParaDeletar() {
        assertThat(AcaoPermissao.porMetodoHttp("DELETE")).isEqualTo(AcaoPermissao.DELETAR);
    }

    @Test
    void metodoHttpDesconhecidoLancaExcecao() {
        assertThatThrownBy(() -> AcaoPermissao.porMetodoHttp("TRACE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
