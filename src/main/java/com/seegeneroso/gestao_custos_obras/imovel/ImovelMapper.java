package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.enums.StatusImovel;
import org.springframework.stereotype.Component;

@Component
public class ImovelMapper {

    public ImovelModel toEntity(ImovelRequestDTO dto) {
        return ImovelModel.builder()
                .identificador(dto.identificador())
                .tipo(dto.tipo())
                .endereco(dto.endereco())
                .area(dto.area())
                .valorAquisicaoInicial(dto.valorAquisicaoInicial())
                .status(dto.status() != null ? dto.status() : StatusImovel.PLANEJAMENTO)
                .descricao(dto.descricao())
                .build();
    }

    public void updateEntityFromDto(ImovelRequestDTO dto, ImovelModel imovel) {
        imovel.setIdentificador(dto.identificador());
        imovel.setTipo(dto.tipo());
        imovel.setEndereco(dto.endereco());
        imovel.setArea(dto.area());
        imovel.setValorAquisicaoInicial(dto.valorAquisicaoInicial());
        if (dto.status() != null) {
            imovel.setStatus(dto.status());
        }
        imovel.setDescricao(dto.descricao());
    }

    public ImovelResponseDTO toResponseDTO(ImovelModel imovel) {
        return new ImovelResponseDTO(
                imovel.getId(),
                imovel.getIdentificador(),
                imovel.getTipo(),
                imovel.getEndereco(),
                imovel.getArea(),
                imovel.getValorAquisicaoInicial(),
                imovel.getStatus(),
                imovel.getDescricao(),
                imovel.getAtivo()
        );
    }
}