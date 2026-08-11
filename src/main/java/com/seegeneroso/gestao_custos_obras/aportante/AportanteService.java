package com.seegeneroso.gestao_custos_obras.aportante;

import com.seegeneroso.gestao_custos_obras.aportante.dto.AportanteRequestDTO;
import com.seegeneroso.gestao_custos_obras.aportante.dto.AportanteResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AportanteService {

    private final AportanteRepository aportanteRepository;
    private final AportanteMapper aportanteMapper;

    @Transactional
    public AportanteResponseDTO criar(AportanteRequestDTO dto) {
        AportanteModel entity = aportanteMapper.toEntity(dto);
        AportanteModel salvo = aportanteRepository.save(entity);
        return aportanteMapper.toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<AportanteResponseDTO> listarTodos() {
        return aportanteRepository.findByAtivoTrue()
                .stream()
                .map(aportanteMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AportanteResponseDTO buscarPorId(Long id) {
        AportanteModel entity = aportanteRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aportante não encontrado com id: " + id));
        return aportanteMapper.toResponseDTO(entity);
    }

    @Transactional
    public AportanteResponseDTO atualizar(Long id, AportanteRequestDTO dto) {
        AportanteModel entity = aportanteRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aportante não encontrado com id: " + id));

        aportanteMapper.updateEntityFromDto(dto, entity);
        AportanteModel atualizado = aportanteRepository.save(entity);
        return aportanteMapper.toResponseDTO(atualizado);
    }

    @Transactional
    public void inativar(Long id) {
        AportanteModel entity = aportanteRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aportante não encontrado com id: " + id));
        entity.setAtivo(false);
        aportanteRepository.save(entity);
    }
}
