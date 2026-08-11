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
}
