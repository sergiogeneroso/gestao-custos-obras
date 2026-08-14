package com.seegeneroso.gestao_custos_obras.fornecedor;

import com.seegeneroso.gestao_custos_obras.fornecedor.dto.FornecedorRequestDTO;
import com.seegeneroso.gestao_custos_obras.fornecedor.dto.FornecedorResponseDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import org.springframework.stereotype.Component;

@Component
public class FornecedorMapper {

    public FornecedorModel toEntity(FornecedorRequestDTO dto, PessoaModel pessoa) {
        return FornecedorModel.builder()
                .pessoa(pessoa)
                .areaAtuacao(dto.areaAtuacao())
                .observacoes(dto.observacoes())
                .build();
    }

    public void updateEntityFromDto(FornecedorRequestDTO dto, PessoaModel pessoa, FornecedorModel entity) {
        entity.setPessoa(pessoa);
        entity.setAreaAtuacao(dto.areaAtuacao());
        entity.setObservacoes(dto.observacoes());
    }

    public FornecedorResponseDTO toResponseDTO(FornecedorModel entity) {
        return new FornecedorResponseDTO(
                entity.getId(),
                entity.getPessoa() != null ? entity.getPessoa().getId() : null,
                entity.getPessoa() != null ? entity.getPessoa().getNome() : null,
                entity.getAreaAtuacao(),
                entity.getObservacoes(),
                entity.getAtivo()
        );
    }
}
