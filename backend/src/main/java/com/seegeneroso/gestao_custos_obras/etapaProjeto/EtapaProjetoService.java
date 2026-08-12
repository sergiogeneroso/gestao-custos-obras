package com.seegeneroso.gestao_custos_obras.etapaProjeto;

import com.seegeneroso.gestao_custos_obras.etapaProjeto.dto.EtapaProjetoRequestDTO;
import com.seegeneroso.gestao_custos_obras.etapaProjeto.dto.EtapaProjetoResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EtapaProjetoService {

    private final EtapaProjetoRepository etapaProjetoRepository;
    private final EtapaProjetoMapper etapaProjetoMapper;

    @Transactional
    public EtapaProjetoResponseDTO criar(EtapaProjetoRequestDTO dto) {
        etapaProjetoRepository.findByNome(dto.nome()).ifPresent(e -> {
            throw new RegraDeNegocioException("Já existe uma etapa de projeto cadastrada com o nome: " + dto.nome());
        });

        EtapaProjetoModel entity = etapaProjetoMapper.toEntity(dto);
        EtapaProjetoModel salva = etapaProjetoRepository.save(entity);
        return etapaProjetoMapper.toResponseDTO(salva);
    }

    @Transactional(readOnly = true)
    public List<EtapaProjetoResponseDTO> listarTodas() {
        return etapaProjetoRepository.findAll()
                .stream()
                .map(etapaProjetoMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EtapaProjetoResponseDTO buscarPorId(Long id) {
        EtapaProjetoModel entity = etapaProjetoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa de projeto não encontrada com id: " + id));
        return etapaProjetoMapper.toResponseDTO(entity);
    }

    @Transactional
    public EtapaProjetoResponseDTO atualizar(Long id, EtapaProjetoRequestDTO dto) {
        EtapaProjetoModel entity = etapaProjetoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa de projeto não encontrada com id: " + id));

        etapaProjetoRepository.findByNome(dto.nome()).ifPresent(e -> {
            if (!e.getId().equals(id)) {
                throw new RegraDeNegocioException("Já existe outra etapa de projeto cadastrada com o nome: " + dto.nome());
            }
        });

        etapaProjetoMapper.updateEntityFromDto(dto, entity);
        EtapaProjetoModel atualizada = etapaProjetoRepository.save(entity);
        return etapaProjetoMapper.toResponseDTO(atualizada);
    }

    @Transactional
    public void deletar(Long id) {
        if (!etapaProjetoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Etapa de projeto não encontrada com id: " + id);
        }
        etapaProjetoRepository.deleteById(id);
    }
}
