package com.seegeneroso.gestao_custos_obras.aportante;

import com.seegeneroso.gestao_custos_obras.aportante.dto.AportanteRequestDTO;
import com.seegeneroso.gestao_custos_obras.aportante.dto.AportanteResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class AportanteMapper {

    public AportanteModel toEntity(AportanteRequestDTO dto) {
        return AportanteModel.builder()
                .nome(dto.nome())
                .documento(dto.documento())
                .email(dto.email())
                .telefone(dto.telefone())
                .tipoParticipacao(dto.tipoParticipacao())
                .build();
    }

    public void updateEntityFromDto(AportanteRequestDTO dto, AportanteModel entity) {
        entity.setNome(dto.nome());
        entity.setDocumento(dto.documento());
        entity.setEmail(dto.email());
        entity.setTelefone(dto.telefone());
        entity.setTipoParticipacao(dto.tipoParticipacao());
    }

    public AportanteResponseDTO toResponseDTO(AportanteModel entity) {
        return new AportanteResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDocumento(),
                entity.getEmail(),
                entity.getTelefone(),
                entity.getTipoParticipacao(),
                entity.getAtivo()
        );
    }
}
