package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelDocumentoResponseDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFaseRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFotoResponseDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelResponseDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelSituacaoRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoDocumentoImovel;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImovelService {

    private final ImovelRepository imovelRepository;
    private final ImovelFotoRepository imovelFotoRepository;
    private final ImovelDocumentoRepository imovelDocumentoRepository;
    private final PessoaRepository pessoaRepository;
    private final ContratoFinanceiroRepository contratoFinanceiroRepository;
    private final com.seegeneroso.gestao_custos_obras.shared.storage.StorageService storageService;
    private final ImovelMapper imovelMapper;

    @Transactional
    public ImovelResponseDTO criar(ImovelRequestDTO dto) {
        if (imovelRepository.existsByIdentificador(dto.identificador())) {
            throw new RegraDeNegocioException("Já existe um imóvel registrado com o identificador: " + dto.identificador());
        }

        PessoaModel vendedor = buscarPessoaOpcional(dto.compraVendedorId());
        ImovelModel imovel = imovelMapper.toEntity(dto, vendedor);
        ImovelModel imovelSalvo = imovelRepository.save(imovel);
        return imovelMapper.toResponseDTO(imovelSalvo, null);
    }

    @Transactional(readOnly = true)
    public List<ImovelResponseDTO> listarTodos() {
        List<ImovelModel> imoveis = imovelRepository.findByAtivoTrue();
        List<Long> ids = imoveis.stream().map(ImovelModel::getId).toList();
        Map<Long, String> fotosPrincipaisPorImovel = imovelFotoRepository.findByImovelIdInAndPrincipalTrue(ids).stream()
                .collect(Collectors.toMap(foto -> foto.getImovel().getId(), ImovelFotoModel::getUrl));

        return imoveis.stream()
                .map(imovel -> imovelMapper.toResponseDTO(imovel, fotosPrincipaisPorImovel.get(imovel.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ImovelResponseDTO buscarPorId(Long id) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + id));
        return imovelMapper.toResponseDTO(imovel, buscarUrlFotoPrincipal(id));
    }

    @Transactional
    public ImovelResponseDTO atualizar(Long id, ImovelRequestDTO dto) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + id));

        if (!imovel.getIdentificador().equalsIgnoreCase(dto.identificador())
                && imovelRepository.existsByIdentificador(dto.identificador())) {
            throw new RegraDeNegocioException("Já existe outro imóvel registrado com o identificador: " + dto.identificador());
        }

        PessoaModel vendedor = buscarPessoaOpcional(dto.compraVendedorId());
        imovelMapper.updateEntityFromDto(dto, vendedor, imovel);
        ImovelModel imovelAtualizado = imovelRepository.save(imovel);
        return imovelMapper.toResponseDTO(imovelAtualizado, buscarUrlFotoPrincipal(id));
    }

    @Transactional
    public ImovelResponseDTO avancarFase(Long id, ImovelFaseRequestDTO dto) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + id));

        FaseImovel[] ordem = FaseImovel.values();
        int atual = imovel.getFase().ordinal();
        int nova = dto.novaFase().ordinal();

        if (nova != atual + 1) {
            throw new RegraDeNegocioException(
                    "A fase só pode avançar para a próxima da sequência (" + ordem[atual] +
                            (atual + 1 < ordem.length ? " → " + ordem[atual + 1] : "") + ").");
        }

        java.time.LocalDate dataAnterior = switch (dto.novaFase()) {
            case CONSTRUCAO -> imovel.getDataInicioLote();
            case CASA -> imovel.getDataInicioConstrucao();
            case LOTE -> throw new RegraDeNegocioException("LOTE é a fase inicial; não é um destino de transição.");
        };

        if (dataAnterior != null && dto.data().isBefore(dataAnterior)) {
            throw new RegraDeNegocioException("A data da transição não pode ser anterior à data da fase anterior.");
        }

        switch (dto.novaFase()) {
            case CONSTRUCAO -> imovel.setDataInicioConstrucao(dto.data());
            case CASA -> imovel.setDataConclusaoObra(dto.data());
            case LOTE -> { }
        }
        imovel.setFase(dto.novaFase());

        ImovelModel imovelAtualizado = imovelRepository.save(imovel);

        String aviso = dto.novaFase() == FaseImovel.CONSTRUCAO ? avisoParcelamentoCompraAtivo(id) : null;
        return imovelMapper.toResponseDTO(imovelAtualizado, buscarUrlFotoPrincipal(id), aviso);
    }

    private String avisoParcelamentoCompraAtivo(Long imovelId) {
        boolean temParcelamentoCompraAtivo = contratoFinanceiroRepository.findByImovelId(imovelId).stream()
                .anyMatch(c -> c.getTipo() == TipoContratoFinanceiro.PARCELAMENTO_COMPRA
                        && c.getSituacao() == SituacaoContrato.ATIVO);

        return temParcelamentoCompraAtivo
                ? "Este imóvel ainda tem um contrato de PARCELAMENTO_COMPRA ativo. Bancos costumam exigir o terreno quitado para financiar a obra."
                : null;
    }

    @Transactional
    public ImovelResponseDTO alterarSituacao(Long id, ImovelSituacaoRequestDTO dto) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + id));

        if (imovel.getSituacao() == SituacaoImovel.VENDIDO && dto.novaSituacao() != SituacaoImovel.VENDIDO) {
            throw new RegraDeNegocioException("Imóvel já vendido não pode voltar para outra situação.");
        }

        if (dto.novaSituacao() == SituacaoImovel.VENDIDO) {
            if (dto.valorVenda() == null || dto.dataVenda() == null) {
                throw new RegraDeNegocioException("Situação VENDIDO exige valor e data de venda.");
            }
            PessoaModel comprador = buscarPessoaOpcional(dto.compradorId());
            imovel.getVenda().setValor(dto.valorVenda());
            imovel.getVenda().setData(dto.dataVenda());
            imovel.getVenda().setComprador(comprador);
        }

        imovel.setSituacao(dto.novaSituacao());
        ImovelModel imovelAtualizado = imovelRepository.save(imovel);
        return imovelMapper.toResponseDTO(imovelAtualizado, buscarUrlFotoPrincipal(id));
    }

    private PessoaModel buscarPessoaOpcional(Long pessoaId) {
        if (pessoaId == null) {
            return null;
        }
        return pessoaRepository.findByIdAndAtivoTrue(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada com id: " + pessoaId));
    }

    private String buscarUrlFotoPrincipal(Long imovelId) {
        return imovelFotoRepository.findByImovelIdAndPrincipalTrue(imovelId)
                .map(ImovelFotoModel::getUrl)
                .orElse(null);
    }

    @Transactional
    public void inativar(Long id) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + id));
        imovel.setAtivo(false);
        imovelRepository.save(imovel);
    }

    @Transactional
    public ImovelFotoResponseDTO adicionarFoto(Long imovelId, org.springframework.web.multipart.MultipartFile arquivo, String legenda) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(imovelId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId));

        String subpasta = "imoveis/" + imovelId;
        String nomeArquivo = storageService.salvar(arquivo, subpasta);

        String fileUri = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/arquivos/download/")
                .path(subpasta + "/")
                .path(nomeArquivo)
                .toUriString();

        ImovelFotoModel foto = ImovelFotoModel.builder()
                .imovel(imovel)
                .url(fileUri)
                .legenda(legenda)
                .build();

        ImovelFotoModel fotoSalva = imovelFotoRepository.save(foto);
        return toFotoResponseDTO(fotoSalva);
    }

    @Transactional(readOnly = true)
    public List<ImovelFotoResponseDTO> listarFotos(Long imovelId) {
        if (!imovelRepository.existsById(imovelId)) {
            throw new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId);
        }
        return imovelFotoRepository.findByImovelId(imovelId).stream()
                .map(this::toFotoResponseDTO)
                .toList();
    }

    @Transactional
    public List<ImovelFotoResponseDTO> definirFotoPrincipal(Long imovelId, Long fotoId) {
        List<ImovelFotoModel> fotos = imovelFotoRepository.findByImovelId(imovelId);
        boolean encontrada = fotos.stream().anyMatch(foto -> foto.getId().equals(fotoId));
        if (!encontrada) {
            throw new RecursoNaoEncontradoException("Foto não encontrada com id: " + fotoId);
        }

        fotos.forEach(foto -> foto.setPrincipal(foto.getId().equals(fotoId)));
        List<ImovelFotoModel> fotosAtualizadas = imovelFotoRepository.saveAll(fotos);
        return fotosAtualizadas.stream().map(this::toFotoResponseDTO).toList();
    }

    private ImovelFotoResponseDTO toFotoResponseDTO(ImovelFotoModel foto) {
        return new ImovelFotoResponseDTO(
                foto.getId(),
                foto.getImovel().getId(),
                foto.getUrl(),
                foto.getLegenda(),
                foto.getDataUpload(),
                foto.getPrincipal()
        );
    }

    @Transactional
    public void deletarFoto(Long imovelId, Long fotoId) {
        ImovelFotoModel foto = imovelFotoRepository.findById(fotoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Foto não encontrada com id: " + fotoId));

        if (!foto.getImovel().getId().equals(imovelId)) {
            throw new RegraDeNegocioException("A foto não pertence ao imóvel informado.");
        }

        imovelFotoRepository.delete(foto);
    }

    @Transactional
    public ImovelDocumentoResponseDTO adicionarDocumento(
            Long imovelId, org.springframework.web.multipart.MultipartFile arquivo,
            TipoDocumentoImovel tipoDocumento, FaseImovel faseImovel) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(imovelId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId));

        String subpasta = "imoveis/" + imovelId + "/documentos";
        String nomeArquivo = storageService.salvar(arquivo, subpasta);

        String fileUri = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/arquivos/download/")
                .path(subpasta + "/")
                .path(nomeArquivo)
                .toUriString();

        ImovelDocumentoModel documento = ImovelDocumentoModel.builder()
                .imovel(imovel)
                .tipoDocumento(tipoDocumento)
                .faseImovel(faseImovel != null ? faseImovel : imovel.getFase())
                .url(fileUri)
                .build();

        ImovelDocumentoModel documentoSalvo = imovelDocumentoRepository.save(documento);
        return toDocumentoResponseDTO(documentoSalvo);
    }

    @Transactional(readOnly = true)
    public List<ImovelDocumentoResponseDTO> listarDocumentos(Long imovelId, TipoDocumentoImovel tipoDocumento) {
        if (!imovelRepository.existsById(imovelId)) {
            throw new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId);
        }
        List<ImovelDocumentoModel> documentos = tipoDocumento != null
                ? imovelDocumentoRepository.findByImovelIdAndTipoDocumento(imovelId, tipoDocumento)
                : imovelDocumentoRepository.findByImovelId(imovelId);
        return documentos.stream().map(this::toDocumentoResponseDTO).toList();
    }

    @Transactional
    public void deletarDocumento(Long imovelId, Long documentoId) {
        ImovelDocumentoModel documento = imovelDocumentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado com id: " + documentoId));

        if (!documento.getImovel().getId().equals(imovelId)) {
            throw new RegraDeNegocioException("O documento não pertence ao imóvel informado.");
        }

        imovelDocumentoRepository.delete(documento);
    }

    private ImovelDocumentoResponseDTO toDocumentoResponseDTO(ImovelDocumentoModel documento) {
        return new ImovelDocumentoResponseDTO(
                documento.getId(),
                documento.getImovel().getId(),
                documento.getTipoDocumento(),
                documento.getFaseImovel(),
                documento.getUrl(),
                documento.getDataUpload()
        );
    }
}
