package com.seegeneroso.gestao_custos_obras.etapaProjeto;

import com.seegeneroso.gestao_custos_obras.etapaProjeto.dto.EtapaProjetoRequestDTO;
import com.seegeneroso.gestao_custos_obras.etapaProjeto.dto.EtapaProjetoResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class EtapaProjetoMapper {

    public EtapaProjetoModel toEntity(EtapaProjetoRequestDTO dto) {
        return EtapaProjetoModel.builder()
                .nome(dto.nome())
                .descricao(dto.descricao())
                .build();
    }

    public void updateEntityFromDto(EtapaProjetoRequestDTO dto, EtapaProjetoModel entity) {
        entity.setNome(dto.nome());
        entity.setDescricao(dto.descricao());
    }

    public EtapaProjetoResponseDTO toResponseDTO(EtapaProjetoModel entity) {
        return new EtapaProjetoResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao()
        );
    }
}
