package com.seegeneroso.gestao_custos_obras.categoriaDespesa;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.dto.CategoriaDespesaRequestDTO;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.dto.CategoriaDespesaResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaDespesaService {

    private final CategoriaDespesaRepository categoriaDespesaRepository;
    private final CategoriaDespesaMapper categoriaDespesaMapper;

    @Transactional
    public CategoriaDespesaResponseDTO criar(CategoriaDespesaRequestDTO dto) {
        categoriaDespesaRepository.findByNome(dto.nome()).ifPresent(e -> {
            throw new RegraDeNegocioException("Já existe uma categoria de despesa cadastrada com o nome: " + dto.nome());
        });

        CategoriaDespesaModel entity = categoriaDespesaMapper.toEntity(dto);
        CategoriaDespesaModel salva = categoriaDespesaRepository.save(entity);
        return categoriaDespesaMapper.toResponseDTO(salva);
    }

    @Transactional(readOnly = true)
    public List<CategoriaDespesaResponseDTO> listarTodas() {
        return categoriaDespesaRepository.findAll()
                .stream()
                .map(categoriaDespesaMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaDespesaResponseDTO buscarPorId(Long id) {
        CategoriaDespesaModel entity = categoriaDespesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria de despesa não encontrada com id: " + id));
        return categoriaDespesaMapper.toResponseDTO(entity);
    }

    @Transactional
    public CategoriaDespesaResponseDTO atualizar(Long id, CategoriaDespesaRequestDTO dto) {
        CategoriaDespesaModel entity = categoriaDespesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria de despesa não encontrada com id: " + id));

        categoriaDespesaRepository.findByNome(dto.nome()).ifPresent(e -> {
            if (!e.getId().equals(id)) {
                throw new RegraDeNegocioException("Já existe outra categoria de despesa cadastrada com o nome: " + dto.nome());
            }
        });

        categoriaDespesaMapper.updateEntityFromDto(dto, entity);
        CategoriaDespesaModel atualizada = categoriaDespesaRepository.save(entity);
        return categoriaDespesaMapper.toResponseDTO(atualizada);
    }

    @Transactional
    public void deletar(Long id) {
        if (!categoriaDespesaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Categoria de despesa não encontrada com id: " + id);
        }
        categoriaDespesaRepository.deleteById(id);
    }
}
