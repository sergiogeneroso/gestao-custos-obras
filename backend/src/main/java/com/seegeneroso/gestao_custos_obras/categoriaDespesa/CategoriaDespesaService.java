package com.seegeneroso.gestao_custos_obras.categoriaDespesa;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.dto.CategoriaDespesaRequestDTO;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.dto.CategoriaDespesaResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.Buscas;
import com.seegeneroso.gestao_custos_obras.shared.PaginaDTO;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.OperacaoAuditoria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaDespesaService {

    private final CategoriaDespesaRepository categoriaDespesaRepository;
    private final CategoriaDespesaMapper categoriaDespesaMapper;
    private final AuditoriaService auditoriaService;

    @Transactional
    public CategoriaDespesaResponseDTO criar(CategoriaDespesaRequestDTO dto) {
        categoriaDespesaRepository.findByNome(dto.nome()).ifPresent(e -> {
            throw new RegraDeNegocioException("Já existe uma categoria de despesa cadastrada com o nome: " + dto.nome());
        });

        CategoriaDespesaModel entity = categoriaDespesaMapper.toEntity(dto);
        CategoriaDespesaModel salva = categoriaDespesaRepository.save(entity);
        CategoriaDespesaResponseDTO responseDto = categoriaDespesaMapper.toResponseDTO(salva);
        auditoriaService.registrar("CategoriaDespesa", salva.getId(), OperacaoAuditoria.CRIACAO, null, responseDto);
        return responseDto;
    }

    /**
     * Lista completa, sem paginar. É o que alimenta os combos de categoria dos formulários —
     * um select paginado esconderia opções válidas. A tela de listagem usa {@link #buscar}.
     */
    @Transactional(readOnly = true)
    public List<CategoriaDespesaResponseDTO> listarTodas() {
        return categoriaDespesaRepository.findAll()
                .stream()
                .map(categoriaDespesaMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<CategoriaDespesaResponseDTO> buscar(String busca, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("nome"));
        return PaginaDTO.de(categoriaDespesaRepository.buscar(Buscas.normalizar(busca), pageable),
                categoriaDespesaMapper::toResponseDTO);
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
        CategoriaDespesaResponseDTO estadoAnterior = categoriaDespesaMapper.toResponseDTO(entity);

        categoriaDespesaRepository.findByNome(dto.nome()).ifPresent(e -> {
            if (!e.getId().equals(id)) {
                throw new RegraDeNegocioException("Já existe outra categoria de despesa cadastrada com o nome: " + dto.nome());
            }
        });

        categoriaDespesaMapper.updateEntityFromDto(dto, entity);
        CategoriaDespesaModel atualizada = categoriaDespesaRepository.save(entity);
        CategoriaDespesaResponseDTO estadoNovo = categoriaDespesaMapper.toResponseDTO(atualizada);
        auditoriaService.registrar("CategoriaDespesa", id, OperacaoAuditoria.EDICAO, estadoAnterior, estadoNovo);
        return estadoNovo;
    }

    @Transactional
    public void deletar(Long id) {
        CategoriaDespesaModel entity = categoriaDespesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria de despesa não encontrada com id: " + id));
        CategoriaDespesaResponseDTO estadoAnterior = categoriaDespesaMapper.toResponseDTO(entity);
        categoriaDespesaRepository.deleteById(id);
        auditoriaService.registrar("CategoriaDespesa", id, OperacaoAuditoria.EXCLUSAO, estadoAnterior, null);
    }
}
