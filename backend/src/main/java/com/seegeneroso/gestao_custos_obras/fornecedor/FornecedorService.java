package com.seegeneroso.gestao_custos_obras.fornecedor;

import com.seegeneroso.gestao_custos_obras.fornecedor.dto.FornecedorRequestDTO;
import com.seegeneroso.gestao_custos_obras.fornecedor.dto.FornecedorResponseDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final PessoaRepository pessoaRepository;
    private final FornecedorMapper fornecedorMapper;

    @Transactional
    public FornecedorResponseDTO criar(FornecedorRequestDTO dto) {
        PessoaModel pessoa = pessoaRepository.findByIdAndAtivoTrue(dto.pessoaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada com id: " + dto.pessoaId()));

        if (fornecedorRepository.existsByPessoaId(dto.pessoaId())) {
            throw new RegraDeNegocioException("Já existe um fornecedor cadastrado para esta pessoa.");
        }

        FornecedorModel entity = fornecedorMapper.toEntity(dto, pessoa);
        FornecedorModel salvo = fornecedorRepository.save(entity);
        return fornecedorMapper.toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponseDTO> listarTodos() {
        return fornecedorRepository.findByAtivoTrue()
                .stream()
                .map(fornecedorMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public FornecedorResponseDTO buscarPorId(Long id) {
        FornecedorModel entity = fornecedorRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado com id: " + id));
        return fornecedorMapper.toResponseDTO(entity);
    }

    @Transactional
    public FornecedorResponseDTO atualizar(Long id, FornecedorRequestDTO dto) {
        FornecedorModel entity = fornecedorRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado com id: " + id));

        PessoaModel pessoa = pessoaRepository.findByIdAndAtivoTrue(dto.pessoaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada com id: " + dto.pessoaId()));

        if (!entity.getPessoa().getId().equals(dto.pessoaId())
                && fornecedorRepository.existsByPessoaId(dto.pessoaId())) {
            throw new RegraDeNegocioException("Já existe um fornecedor cadastrado para esta pessoa.");
        }

        fornecedorMapper.updateEntityFromDto(dto, pessoa, entity);
        FornecedorModel atualizado = fornecedorRepository.save(entity);
        return fornecedorMapper.toResponseDTO(atualizado);
    }

    @Transactional
    public void inativar(Long id) {
        FornecedorModel entity = fornecedorRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado com id: " + id));
        entity.setAtivo(false);
        fornecedorRepository.save(entity);
    }
}
