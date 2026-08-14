package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelResponseDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import org.springframework.stereotype.Component;

@Component
public class ImovelMapper {

    public ImovelModel toEntity(ImovelRequestDTO dto, PessoaModel vendedor) {
        DadosCompra compra = DadosCompra.builder()
                .valor(dto.compraValor())
                .data(dto.compraData())
                .vendedor(vendedor)
                .build();

        DadosVenda venda = DadosVenda.builder()
                .valorPretendido(dto.vendaValorPretendido())
                .build();

        return ImovelModel.builder()
                .identificador(dto.identificador())
                .endereco(dto.endereco())
                .area(dto.area())
                .dataInicioLote(dto.dataInicioLote())
                .dataInicioConstrucao(dto.dataInicioConstrucao())
                .dataConclusaoObra(dto.dataConclusaoObra())
                .custoEstimadoObra(dto.custoEstimadoObra())
                .previsaoConclusao(dto.previsaoConclusao())
                .compra(compra)
                .venda(venda)
                .descricao(dto.descricao())
                .build();
    }

    public void updateEntityFromDto(ImovelRequestDTO dto, PessoaModel vendedor, ImovelModel imovel) {
        imovel.setIdentificador(dto.identificador());
        imovel.setEndereco(dto.endereco());
        imovel.setArea(dto.area());
        imovel.setDataInicioLote(dto.dataInicioLote());
        imovel.setDataInicioConstrucao(dto.dataInicioConstrucao());
        imovel.setDataConclusaoObra(dto.dataConclusaoObra());
        imovel.setCustoEstimadoObra(dto.custoEstimadoObra());
        imovel.setPrevisaoConclusao(dto.previsaoConclusao());
        imovel.setDescricao(dto.descricao());

        imovel.getCompra().setValor(dto.compraValor());
        imovel.getCompra().setData(dto.compraData());
        imovel.getCompra().setVendedor(vendedor);

        imovel.getVenda().setValorPretendido(dto.vendaValorPretendido());
    }

    public ImovelResponseDTO toResponseDTO(ImovelModel imovel, String fotoPrincipalUrl) {
        return toResponseDTO(imovel, fotoPrincipalUrl, null);
    }

    public ImovelResponseDTO toResponseDTO(ImovelModel imovel, String fotoPrincipalUrl, String aviso) {
        DadosCompra compra = imovel.getCompra();
        DadosVenda venda = imovel.getVenda();

        return new ImovelResponseDTO(
                imovel.getId(),
                imovel.getIdentificador(),
                imovel.getFase(),
                imovel.getSituacao(),
                imovel.getEndereco(),
                imovel.getArea(),
                imovel.getDataInicioLote(),
                imovel.getDataInicioConstrucao(),
                imovel.getDataConclusaoObra(),
                imovel.getCustoEstimadoObra(),
                imovel.getPrevisaoConclusao(),
                compra != null ? compra.getValor() : null,
                compra != null ? compra.getData() : null,
                compra != null && compra.getVendedor() != null ? compra.getVendedor().getId() : null,
                compra != null && compra.getVendedor() != null ? compra.getVendedor().getNome() : null,
                venda != null ? venda.getValor() : null,
                venda != null ? venda.getData() : null,
                venda != null && venda.getComprador() != null ? venda.getComprador().getId() : null,
                venda != null && venda.getComprador() != null ? venda.getComprador().getNome() : null,
                venda != null ? venda.getValorPretendido() : null,
                imovel.getDescricao(),
                imovel.getAtivo(),
                fotoPrincipalUrl,
                aviso
        );
    }
}
