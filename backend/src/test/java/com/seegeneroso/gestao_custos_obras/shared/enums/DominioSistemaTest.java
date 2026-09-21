package com.seegeneroso.gestao_custos_obras.shared.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Cobre a derivação domínio <- primeiro segmento de path que o PermissaoInterceptor usa
// (ADR-046) — lista fixa no código, não administrável em runtime.
class DominioSistemaTest {

    @Test
    void prefixoConhecidoResolveODominio() {
        assertThat(DominioSistema.porPrefixoUrl("despesas")).contains(DominioSistema.DESPESA);
        assertThat(DominioSistema.porPrefixoUrl("contratos-financeiros")).contains(DominioSistema.CONTRATO_FINANCEIRO);
        assertThat(DominioSistema.porPrefixoUrl("perfis")).contains(DominioSistema.PERFIL);
    }

    @Test
    void prefixoDesconhecidoNaoResolveNada() {
        assertThat(DominioSistema.porPrefixoUrl("arquivos")).isEmpty();
        assertThat(DominioSistema.porPrefixoUrl("nao-existe")).isEmpty();
    }
}
