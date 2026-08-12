package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        return imovelMapper.toResponseDTO(imovelSalvo);
    }

    @Transactional(readOnly = true)
    public List<ImovelResponseDTO> listarTodos() {
        return imovelRepository.findByAtivoTrue()
                .stream()
                .map(imovelMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImovelResponseDTO buscarPorId(Long id) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + id));
        return imovelMapper.toResponseDTO(imovel);
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
        return imovelMapper.toResponseDTO(imovelAtualizado);
    }

    @Transactional
    public void inativar(Long id) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + id));
        imovel.setAtivo(false);
        imovelRepository.save(imovel);
    }

    @Transactional
    public com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFotoResponseDTO adicionarFoto(Long imovelId, org.springframework.web.multipart.MultipartFile arquivo, String legenda) {
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
        return new com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFotoResponseDTO(
                fotoSalva.getId(),
                imovel.getId(),
                fotoSalva.getUrl(),
                fotoSalva.getLegenda(),
                fotoSalva.getDataUpload()
        );
    }

    @Transactional(readOnly = true)
    public List<com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFotoResponseDTO> listarFotos(Long imovelId) {
        if (!imovelRepository.existsById(imovelId)) {
            throw new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId);
        }
        return imovelFotoRepository.findByImovelId(imovelId).stream()
                .map(foto -> new com.seegeneroso.gestao_custos_obras.imovel.dto.ImovelFotoResponseDTO(
                        foto.getId(),
                        foto.getImovel().getId(),
                        foto.getUrl(),
                        foto.getLegenda(),
                        foto.getDataUpload()
                ))
                .toList();
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
