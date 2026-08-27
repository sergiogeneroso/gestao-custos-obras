package com.seegeneroso.gestao_custos_obras.shared.validacao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentosTest {

    @Test
    void aceitaCpfValidoComOuSemPontuacao() {
        assertThat(Documentos.cpfValido("529.982.247-25")).isTrue();
        assertThat(Documentos.cpfValido("52998224725")).isTrue();
    }

    @Test
    void recusaCpfComDigitoErradoTamanhoErradoOuTodoIgual() {
        assertThat(Documentos.cpfValido("529.982.247-26")).isFalse();
        assertThat(Documentos.cpfValido("5299822472")).isFalse();
        assertThat(Documentos.cpfValido("111.111.111-11")).isFalse();
        assertThat(Documentos.cpfValido(null)).isFalse();
    }

    @Test
    void aceitaCnpjNumericoValidoComOuSemPontuacao() {
        assertThat(Documentos.cnpjValido("11.222.333/0001-81")).isTrue();
        assertThat(Documentos.cnpjValido("11222333000181")).isTrue();
    }

    // O CNPJ alfanumérico passou a ser emitido em 2026 e usa o mesmo algoritmo,
    // com cada caractere valendo ASCII - 48.
    @Test
    void aceitaCnpjAlfanumericoValido() {
        assertThat(Documentos.cnpjValido("12.ABC.345/01DE-35")).isTrue();
    }

    @Test
    void recusaCnpjComDigitoErradoOuTodoIgual() {
        assertThat(Documentos.cnpjValido("11.222.333/0001-82")).isFalse();
        assertThat(Documentos.cnpjValido("00000000000000")).isFalse();
        assertThat(Documentos.cnpjValido("112223330001")).isFalse();
    }

    @Test
    void normalizaTirandoPontuacaoEsubindoParaMaiusculas() {
        assertThat(Documentos.normalizar("12.abc.345/01de-35")).isEqualTo("12ABC34501DE35");
        assertThat(Documentos.apenasDigitos("(11) 98765-4321")).isEqualTo("11987654321");
    }
}
