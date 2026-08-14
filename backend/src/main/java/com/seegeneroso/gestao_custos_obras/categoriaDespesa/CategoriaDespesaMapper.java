package com.seegeneroso.gestao_custos_obras.categoriaDespesa;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.dto.CategoriaDespesaRequestDTO;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.dto.CategoriaDespesaResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class CategoriaDespesaMapper {

    public CategoriaDespesaModel toEntity(CategoriaDespesaRequestDTO dto) {
        return CategoriaDespesaModel.builder()
                .nome(dto.nome())
                .descricao(dto.descricao())
                .build();
    }

    public void updateEntityFromDto(CategoriaDespesaRequestDTO dto, CategoriaDespesaModel entity) {
        entity.setNome(dto.nome());
        entity.setDescricao(dto.descricao());
    }

    public CategoriaDespesaResponseDTO toResponseDTO(CategoriaDespesaModel entity) {
        return new CategoriaDespesaResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao()
        );
    }
}
