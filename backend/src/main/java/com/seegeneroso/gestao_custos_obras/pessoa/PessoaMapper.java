package com.seegeneroso.gestao_custos_obras.pessoa;

import com.seegeneroso.gestao_custos_obras.pessoa.dto.PessoaRequestDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.dto.PessoaResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.validacao.Documentos;
import org.springframework.stereotype.Component;

// Documento e telefone são gravados sem pontuação (ver Documentos): a máscara é da tela, e
// documento normalizado é o que faz a checagem de duplicidade de PessoaService valer.
@Component
public class PessoaMapper {

    public PessoaModel toEntity(PessoaRequestDTO dto) {
        return PessoaModel.builder()
                .nome(dto.nome())
                .tipoPessoa(dto.tipoPessoa())
                .documento(Documentos.normalizar(dto.documento()))
                .email(dto.email())
                .telefone(Documentos.apenasDigitos(dto.telefone()))
                .fornecedor(Boolean.TRUE.equals(dto.fornecedor()))
                .areaAtuacao(dto.areaAtuacao())
                .observacoes(dto.observacoes())
                .build();
    }

    public void updateEntityFromDto(PessoaRequestDTO dto, PessoaModel entity) {
        entity.setNome(dto.nome());
        entity.setTipoPessoa(dto.tipoPessoa());
        entity.setDocumento(Documentos.normalizar(dto.documento()));
        entity.setEmail(dto.email());
        entity.setTelefone(Documentos.apenasDigitos(dto.telefone()));
        entity.setFornecedor(Boolean.TRUE.equals(dto.fornecedor()));
        entity.setAreaAtuacao(dto.areaAtuacao());
        entity.setObservacoes(dto.observacoes());
    }

    public PessoaResponseDTO toResponseDTO(PessoaModel entity) {
        return new PessoaResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getTipoPessoa(),
                entity.getDocumento(),
                entity.getEmail(),
                entity.getTelefone(),
                entity.getFornecedor(),
                entity.getAreaAtuacao(),
                entity.getObservacoes(),
                entity.getExclusao().getAtivo()
        );
    }
}
