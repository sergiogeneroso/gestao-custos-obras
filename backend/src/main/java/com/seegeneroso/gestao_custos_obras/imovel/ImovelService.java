package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFotoResponseDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelResponseDTO;
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
    private final com.seegeneroso.gestao_custos_obras.shared.storage.StorageService storageService;
    private final ImovelMapper imovelMapper;

    @Transactional
    public ImovelResponseDTO criar(ImovelRequestDTO dto) {
        if (imovelRepository.existsByIdentificador(dto.identificador())) {
            throw new RegraDeNegocioException("Já existe um imóvel registrado com o identificador: " + dto.identificador());
        }

        ImovelModel imovel = imovelMapper.toEntity(dto);
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

        imovelMapper.updateEntityFromDto(dto, imovel);
        ImovelModel imovelAtualizado = imovelRepository.save(imovel);
        return imovelMapper.toResponseDTO(imovelAtualizado, buscarUrlFotoPrincipal(id));
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
}
