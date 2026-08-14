package com.seegeneroso.gestao_custos_obras.pessoa;

import com.seegeneroso.gestao_custos_obras.pessoa.dto.PessoaRequestDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.dto.PessoaResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final PessoaMapper pessoaMapper;

    // ponytail: documento sem validação de dígito verificador (CPF/CNPJ); entra se dado sujo incomodar (ADR-021)
    @Transactional
    public PessoaResponseDTO criar(PessoaRequestDTO dto) {
        if (pessoaRepository.existsByDocumento(dto.documento())) {
            throw new RegraDeNegocioException("Já existe uma pessoa cadastrada com o documento: " + dto.documento());
        }

        PessoaModel entity = pessoaMapper.toEntity(dto);
        PessoaModel salvo = pessoaRepository.save(entity);
        return pessoaMapper.toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<PessoaResponseDTO> listarTodos() {
        return pessoaRepository.findByAtivoTrue()
                .stream()
                .map(pessoaMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponseDTO buscarPorId(Long id) {
        PessoaModel entity = pessoaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada com id: " + id));
        return pessoaMapper.toResponseDTO(entity);
    }

    @Transactional
    public PessoaResponseDTO atualizar(Long id, PessoaRequestDTO dto) {
        PessoaModel entity = pessoaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada com id: " + id));

        if (!entity.getDocumento().equalsIgnoreCase(dto.documento())
                && pessoaRepository.existsByDocumento(dto.documento())) {
            throw new RegraDeNegocioException("Já existe outra pessoa cadastrada com o documento: " + dto.documento());
        }

        pessoaMapper.updateEntityFromDto(dto, entity);
        PessoaModel atualizado = pessoaRepository.save(entity);
        return pessoaMapper.toResponseDTO(atualizado);
    }

    @Transactional
    public void inativar(Long id) {
        PessoaModel entity = pessoaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada com id: " + id));
        entity.setAtivo(false);
        pessoaRepository.save(entity);
    }
}
