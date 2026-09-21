package com.seegeneroso.gestao_custos_obras.perfil.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// ADR-046: "acessar" é pré-requisito das outras três ações no mesmo domínio — um perfil não pode
// incluir/alterar/deletar algo que ele nem consegue listar.
class PerfilRequestDTOTest {

    @Test
    void permissaoDeAcessarSozinhaEhValida() {
        PerfilRequestDTO dto = dto(new PerfilPermissaoDTO(DominioSistema.DESPESA, AcaoPermissao.ACESSAR));

        assertThat(dto.isAcessarPrerequisitoDasDemais()).isTrue();
    }

    @Test
    void incluirComAcessarNoMesmoDominioEhValido() {
        PerfilRequestDTO dto = dto(
                new PerfilPermissaoDTO(DominioSistema.DESPESA, AcaoPermissao.ACESSAR),
                new PerfilPermissaoDTO(DominioSistema.DESPESA, AcaoPermissao.INCLUIR));

        assertThat(dto.isAcessarPrerequisitoDasDemais()).isTrue();
    }

    @Test
    void incluirSemAcessarNoMesmoDominioEhInvalido() {
        PerfilRequestDTO dto = dto(new PerfilPermissaoDTO(DominioSistema.DESPESA, AcaoPermissao.INCLUIR));

        assertThat(dto.isAcessarPrerequisitoDasDemais()).isFalse();
    }

    @Test
    void acessarEmUmDominioNaoCobreIncluirEmOutro() {
        PerfilRequestDTO dto = dto(
                new PerfilPermissaoDTO(DominioSistema.DESPESA, AcaoPermissao.ACESSAR),
                new PerfilPermissaoDTO(DominioSistema.PESSOA, AcaoPermissao.INCLUIR));

        assertThat(dto.isAcessarPrerequisitoDasDemais()).isFalse();
    }

    @Test
    void listaVaziaEhValida() {
        PerfilRequestDTO dto = new PerfilRequestDTO("Sem acesso", List.of());

        assertThat(dto.isAcessarPrerequisitoDasDemais()).isTrue();
    }

    private PerfilRequestDTO dto(PerfilPermissaoDTO... permissoes) {
        return new PerfilRequestDTO("Operador", List.of(permissoes));
    }
}
