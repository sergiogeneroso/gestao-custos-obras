package com.seegeneroso.gestao_custos_obras.pessoa;

import com.seegeneroso.gestao_custos_obras.pessoa.dto.PessoaRequestDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.dto.PessoaResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class PessoaMapper {

    public PessoaModel toEntity(PessoaRequestDTO dto) {
        return PessoaModel.builder()
                .nome(dto.nome())
                .tipoPessoa(dto.tipoPessoa())
                .documento(dto.documento())
                .email(dto.email())
                .telefone(dto.telefone())
                .build();
    }

    public void updateEntityFromDto(PessoaRequestDTO dto, PessoaModel entity) {
        entity.setNome(dto.nome());
        entity.setTipoPessoa(dto.tipoPessoa());
        entity.setDocumento(dto.documento());
        entity.setEmail(dto.email());
        entity.setTelefone(dto.telefone());
    }

    public PessoaResponseDTO toResponseDTO(PessoaModel entity) {
        return new PessoaResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getTipoPessoa(),
                entity.getDocumento(),
                entity.getEmail(),
                entity.getTelefone(),
                entity.getAtivo()
        );
    }
}
