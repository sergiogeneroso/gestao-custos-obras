package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.imovel.dto.DadosCasaDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.DadosConstrucaoDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.DadosLoteDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelResponseDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import org.springframework.stereotype.Component;

@Component
public class ImovelMapper {

    public ImovelModel toEntity(ImovelRequestDTO dto, PessoaModel vendedor) {
        DadosCompra compra = DadosCompra.builder()
                .valor(dto.compraValor())
                .data(dto.compraData())
                .parcelada(Boolean.TRUE.equals(dto.compraParcelada()))
                .vendedor(vendedor)
                .build();

        DadosVenda venda = DadosVenda.builder()
                .valorPretendido(dto.vendaValorPretendido())
                .build();

        ImovelModel imovel = ImovelModel.builder()
                .identificador(dto.identificador())
                .endereco(dto.endereco())
                .numero(dto.numero())
                .bairro(dto.bairro())
                .cidade(dto.cidade())
                .uf(dto.uf())
                .cep(dto.cep())
                .observacaoEndereco(dto.observacaoEndereco())
                .compra(compra)
                .venda(venda)
                .descricao(dto.descricao())
                .build();

        // Imóvel novo nasce LOTE (ADR-020), então só o grupo do lote é aplicado aqui.
        aplicarLote(dto.lote(), imovel);
        return imovel;
    }

    public void updateEntityFromDto(ImovelRequestDTO dto, PessoaModel vendedor,
                                    PessoaModel responsavelTecnico, ImovelModel imovel) {
        imovel.setIdentificador(dto.identificador());
        imovel.setEndereco(dto.endereco());
        imovel.setNumero(dto.numero());
        imovel.setBairro(dto.bairro());
        imovel.setCidade(dto.cidade());
        imovel.setUf(dto.uf());
        imovel.setCep(dto.cep());
        imovel.setObservacaoEndereco(dto.observacaoEndereco());
        imovel.setDescricao(dto.descricao());

        imovel.getCompra().setValor(dto.compraValor());
        imovel.getCompra().setData(dto.compraData());
        imovel.getCompra().setParcelada(Boolean.TRUE.equals(dto.compraParcelada()));
        imovel.getCompra().setVendedor(vendedor);

        imovel.getVenda().setValorPretendido(dto.vendaValorPretendido());

        aplicarLote(dto.lote(), imovel);

        // A edição corrige dado de fase já vivida, nunca preenche fase futura (ADR-033): quem não
        // chegou na construção não grava alvará pelo PUT, e sim pela transição de fase.
        if (alcancou(imovel, FaseImovel.CONSTRUCAO)) {
            aplicarConstrucao(dto.construcao(), responsavelTecnico, imovel);
        }
        if (alcancou(imovel, FaseImovel.CASA)) {
            aplicarCasa(dto.casa(), imovel);
        }
    }

    public boolean alcancou(ImovelModel imovel, FaseImovel fase) {
        return imovel.getFase().ordinal() >= fase.ordinal();
    }

    public void aplicarLote(DadosLoteDTO dto, ImovelModel imovel) {
        if (dto == null) {
            return;
        }
        DadosLote lote = imovel.getLote();
        lote.setMatricula(dto.matricula());
        lote.setCartorio(dto.cartorio());
        lote.setDataRegistro(dto.dataRegistro());
        lote.setInscricaoMunicipal(dto.inscricaoMunicipal());
        lote.setArea(dto.area());
    }

    public void aplicarConstrucao(DadosConstrucaoDTO dto, PessoaModel responsavelTecnico, ImovelModel imovel) {
        if (dto == null) {
            return;
        }
        DadosConstrucao construcao = imovel.getConstrucao();
        construcao.setArea(dto.area());
        construcao.setPrevisaoConclusao(dto.previsaoConclusao());
        construcao.setCustoEstimado(dto.custoEstimado());
        construcao.setAlvaraNumero(dto.alvaraNumero());
        construcao.setAlvaraEmissao(dto.alvaraEmissao());
        construcao.setAlvaraValidade(dto.alvaraValidade());
        construcao.setArtNumero(dto.artNumero());
        construcao.setResponsavelTecnico(responsavelTecnico);
        construcao.setCno(dto.cno());

        // dataInicio não vem daqui na transição: quem grava é avancarFase, com a data do fato.
        if (dto.dataInicio() != null) {
            construcao.setDataInicio(dto.dataInicio());
        }
    }

    public void aplicarCasa(DadosCasaDTO dto, ImovelModel imovel) {
        if (dto == null) {
            return;
        }
        DadosCasa casa = imovel.getCasa();
        casa.setHabiteSeNumero(dto.habiteSeNumero());
        casa.setHabiteSeData(dto.habiteSeData());
        casa.setDataAverbacao(dto.dataAverbacao());
        casa.setQuartos(dto.quartos());
        casa.setSuites(dto.suites());
        casa.setBanheiros(dto.banheiros());
        casa.setVagasGaragem(dto.vagasGaragem());

        if (dto.dataConclusaoObra() != null) {
            casa.setDataConclusaoObra(dto.dataConclusaoObra());
        }
    }

    public ImovelResponseDTO toResponseDTO(ImovelModel imovel, String fotoPrincipalUrl) {
        return toResponseDTO(imovel, fotoPrincipalUrl, null);
    }

    public ImovelResponseDTO toResponseDTO(ImovelModel imovel, String fotoPrincipalUrl, String aviso) {
        DadosCompra compra = imovel.getCompra();
        DadosVenda venda = imovel.getVenda();
        DadosLote lote = imovel.getLote();
        DadosConstrucao construcao = imovel.getConstrucao();
        DadosCasa casa = imovel.getCasa();
        PessoaModel responsavelTecnico = construcao.getResponsavelTecnico();

        return new ImovelResponseDTO(
                imovel.getId(),
                imovel.getIdentificador(),
                imovel.getFase(),
                imovel.getSituacao(),
                imovel.getEndereco(),
                imovel.getNumero(),
                imovel.getBairro(),
                imovel.getCidade(),
                imovel.getUf(),
                imovel.getCep(),
                imovel.getObservacaoEndereco(),
                new DadosLoteDTO(
                        lote.getMatricula(), lote.getCartorio(), lote.getDataRegistro(),
                        lote.getInscricaoMunicipal(), lote.getArea()),
                new DadosConstrucaoDTO(
                        construcao.getArea(), construcao.getDataInicio(), construcao.getPrevisaoConclusao(),
                        construcao.getCustoEstimado(), construcao.getAlvaraNumero(), construcao.getAlvaraEmissao(),
                        construcao.getAlvaraValidade(), construcao.getArtNumero(),
                        responsavelTecnico != null ? responsavelTecnico.getId() : null,
                        responsavelTecnico != null ? responsavelTecnico.getNome() : null,
                        construcao.getCno()),
                new DadosCasaDTO(
                        casa.getDataConclusaoObra(), casa.getHabiteSeNumero(), casa.getHabiteSeData(),
                        casa.getDataAverbacao(), casa.getQuartos(), casa.getSuites(),
                        casa.getBanheiros(), casa.getVagasGaragem()),
                compra.getValor(),
                compra.getData(),
                compra.getVendedor() != null ? compra.getVendedor().getId() : null,
                compra.getVendedor() != null ? compra.getVendedor().getNome() : null,
                compra.getParcelada(),
                venda.getValor(),
                venda.getData(),
                venda.getComprador() != null ? venda.getComprador().getId() : null,
                venda.getComprador() != null ? venda.getComprador().getNome() : null,
                venda.getValorPretendido(),
                imovel.getDescricao(),
                imovel.getAtivo(),
                fotoPrincipalUrl,
                aviso
        );
    }
}
