package com.seegeneroso.gestao_custos_obras.shared.config;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import com.seegeneroso.gestao_custos_obras.perfil.PerfilService;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;
import com.seegeneroso.gestao_custos_obras.shared.exception.AcessoNegadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Enforcement do RBAC por domínio (ADR-046): deriva domínio (do segmento de path logo após
 * "/api/") e ação (do verbo HTTP) sem exigir anotação em cada endpoint, e checa a matriz de
 * permissões do perfil do usuário autenticado. Roda como HandlerInterceptor (não como filtro de
 * Spring Security) justamente para poder lançar uma exceção comum e deixar o
 * {@code ApiErrorHandler} formatar o 403 no mesmo formato dos outros erros da API.
 */
@Component
@RequiredArgsConstructor
public class PermissaoInterceptor implements HandlerInterceptor {

    // Endpoints transversais que não são "domínio de negócio" no sentido do RBAC — auth é
    // público/anterior à autenticação, arquivos e orcamentos-categoria (módulo congelado,
    // ADR-029) exigem só estar autenticado, mesma fronteira que ADR-042 já usa para sub-recursos
    // auxiliares.
    private static final Set<String> PREFIXOS_SEM_DOMINIO = Set.of("auth", "arquivos", "orcamentos-categoria");

    // Qualquer usuário autenticado consulta as PRÓPRIAS permissões, independente de ter acesso
    // ao domínio "perfil" — ver PerfilController.minhasPermissoes.
    private static final String PATH_MINHAS_PERMISSOES = "/api/perfis/minhas-permissoes";

    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final PerfilService perfilService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        if (!path.startsWith("/api/") || path.equals(PATH_MINHAS_PERMISSOES) || "OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String prefixo = primeiroSegmento(path.substring("/api/".length()));
        if (PREFIXOS_SEM_DOMINIO.contains(prefixo)) {
            return true;
        }

        DominioSistema dominio = DominioSistema.porPrefixoUrl(prefixo)
                .orElseThrow(() -> new IllegalStateException("Domínio de URL sem mapeamento de permissão: " + prefixo));
        AcaoPermissao acao = AcaoPermissao.porMetodoHttp(request.getMethod());

        UsuarioModel usuario = usuarioAutenticadoService.usuarioAtual();
        if (!perfilService.possuiPermissao(usuario, dominio, acao)) {
            throw new AcessoNegadoException("Seu perfil não tem permissão para " + acao + " em " + dominio + ".");
        }
        return true;
    }

    private String primeiroSegmento(String pathAposApi) {
        int barra = pathAposApi.indexOf('/');
        return barra >= 0 ? pathAposApi.substring(0, barra) : pathAposApi;
    }
}
