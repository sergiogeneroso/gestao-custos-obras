package com.seegeneroso.gestao_custos_obras.perfil.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record PerfilRequestDTO(
        @NotBlank(message = "Nome do perfil é obrigatório")
        String nome,

        @NotNull(message = "Lista de permissões é obrigatória")
        List<@Valid PerfilPermissaoDTO> permissoes
) {

    // "Acessar" é pré-requisito das outras três ações no mesmo domínio (ADR-046): não faz
    // sentido um perfil poder incluir/alterar/deletar algo que ele nem consegue listar.
    @AssertTrue(message = "Um domínio não pode ter incluir/alterar/deletar sem também ter acessar")
    public boolean isAcessarPrerequisitoDasDemais() {
        if (permissoes == null) {
            return true;
        }
        Set<DominioSistema> comAcesso = permissoes.stream()
                .filter(p -> p.dominio() != null && p.acao() == AcaoPermissao.ACESSAR)
                .map(PerfilPermissaoDTO::dominio)
                .collect(Collectors.toSet());
        return permissoes.stream()
                .filter(p -> p.dominio() != null && p.acao() != null && p.acao() != AcaoPermissao.ACESSAR)
                .allMatch(p -> comAcesso.contains(p.dominio()));
    }
}
