package com.seegeneroso.gestao_custos_obras.shared.config;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import com.seegeneroso.gestao_custos_obras.perfil.PerfilService;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;
import com.seegeneroso.gestao_custos_obras.shared.exception.AcessoNegadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// Enforcement do RBAC por domínio (ADR-046): deriva domínio+ação do path/verbo sem exigir
// anotação por endpoint. Este é o ponto que decide 403 ou não em toda a API.
@ExtendWith(MockitoExtension.class)
class PermissaoInterceptorTest {

    @Mock
    private UsuarioAutenticadoService usuarioAutenticadoService;
    @Mock
    private PerfilService perfilService;

    @InjectMocks
    private PermissaoInterceptor permissaoInterceptor;

    @Test
    void requisicaoComPermissaoPassa() {
        UsuarioModel usuario = UsuarioModel.builder().id(1L).build();
        when(usuarioAutenticadoService.usuarioAtual()).thenReturn(usuario);
        when(perfilService.possuiPermissao(usuario, DominioSistema.DESPESA, AcaoPermissao.ACESSAR)).thenReturn(true);

        boolean resultado = permissaoInterceptor.preHandle(requisicao("GET", "/api/despesas"), new MockHttpServletResponse(), new Object());

        assertThat(resultado).isTrue();
    }

    @Test
    void requisicaoSemPermissaoLancaAcessoNegado() {
        UsuarioModel usuario = UsuarioModel.builder().id(1L).build();
        when(usuarioAutenticadoService.usuarioAtual()).thenReturn(usuario);
        when(perfilService.possuiPermissao(usuario, DominioSistema.DESPESA, AcaoPermissao.DELETAR)).thenReturn(false);

        assertThatThrownBy(() -> permissaoInterceptor.preHandle(requisicao("DELETE", "/api/despesas/1"), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(AcessoNegadoException.class);
    }

    // PATCH /fase e /situacao do imóvel contam como "alterar" (ADR-046) — provado aqui verificando
    // que o verbo PATCH resolve pra ALTERAR mesmo num sub-recurso do domínio.
    @Test
    void patchEmSubRecursoContaComoAlterarNoDominioPai() {
        UsuarioModel usuario = UsuarioModel.builder().id(1L).build();
        when(usuarioAutenticadoService.usuarioAtual()).thenReturn(usuario);
        when(perfilService.possuiPermissao(usuario, DominioSistema.IMOVEL, AcaoPermissao.ALTERAR)).thenReturn(true);

        boolean resultado = permissaoInterceptor.preHandle(requisicao("PATCH", "/api/imoveis/1/fase"), new MockHttpServletResponse(), new Object());

        assertThat(resultado).isTrue();
    }

    @Test
    void loginNaoExigePermissaoDeDominio() {
        boolean resultado = permissaoInterceptor.preHandle(requisicao("POST", "/api/auth/login"), new MockHttpServletResponse(), new Object());

        assertThat(resultado).isTrue();
    }

    @Test
    void minhasPermissoesNaoExigePermissaoNoDominioPerfil() {
        boolean resultado = permissaoInterceptor.preHandle(requisicao("GET", "/api/perfis/minhas-permissoes"), new MockHttpServletResponse(), new Object());

        assertThat(resultado).isTrue();
    }

    @Test
    void dominioDeUrlSemMapeamentoLancaErroDeConfiguracao() {
        assertThatThrownBy(() -> permissaoInterceptor.preHandle(requisicao("GET", "/api/rota-nova-sem-dominio"), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(IllegalStateException.class);
    }

    private MockHttpServletRequest requisicao(String metodo, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        request.setRequestURI(uri);
        return request;
    }
}
