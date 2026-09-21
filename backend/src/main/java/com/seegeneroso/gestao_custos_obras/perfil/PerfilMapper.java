package com.seegeneroso.gestao_custos_obras.perfil;

import com.seegeneroso.gestao_custos_obras.perfil.dto.PerfilPermissaoDTO;
import com.seegeneroso.gestao_custos_obras.perfil.dto.PerfilRequestDTO;
import com.seegeneroso.gestao_custos_obras.perfil.dto.PerfilResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class PerfilMapper {

    public PerfilModel toEntity(PerfilRequestDTO dto) {
        PerfilModel entity = PerfilModel.builder()
                .nome(dto.nome())
                .build();
        aplicarPermissoes(dto, entity);
        return entity;
    }

    // orphanRemoval=true em PerfilModel.permissoes: limpar e reinserir apaga do banco a matriz
    // anterior por inteiro — sem histórico por linha a proteger (ver PerfilPermissaoModel).
    public void updateEntityFromDto(PerfilRequestDTO dto, PerfilModel entity) {
        entity.setNome(dto.nome());
        entity.getPermissoes().clear();
        aplicarPermissoes(dto, entity);
    }

    private void aplicarPermissoes(PerfilRequestDTO dto, PerfilModel entity) {
        dto.permissoes().forEach(p -> entity.getPermissoes().add(
                PerfilPermissaoModel.builder()
                        .perfil(entity)
                        .dominio(p.dominio())
                        .acao(p.acao())
                        .build()));
    }

    public PerfilResponseDTO toResponseDTO(PerfilModel entity) {
        return new PerfilResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getPermissoes().stream()
                        .map(p -> new PerfilPermissaoDTO(p.getDominio(), p.getAcao()))
                        .toList(),
                entity.getExclusao().getAtivo()
        );
    }
}
