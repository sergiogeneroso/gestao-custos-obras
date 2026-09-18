package com.seegeneroso.gestao_custos_obras.pessoa;

import com.seegeneroso.gestao_custos_obras.pessoa.dto.PessoaRequestDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.dto.PessoaResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.Buscas;
import com.seegeneroso.gestao_custos_obras.shared.PaginaDTO;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import com.seegeneroso.gestao_custos_obras.shared.validacao.Documentos;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.OperacaoAuditoria;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final PessoaMapper pessoaMapper;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final AuditoriaService auditoriaService;

    // O dígito verificador de CPF/CNPJ é checado por PessoaRequestDTO.isDocumentoValido(). Aqui a
    // comparação é sempre sobre o documento normalizado (o mesmo que o mapper grava): comparando a
    // string crua, o mesmo CPF digitado com e sem pontuação entrava como duas pessoas.
    @Transactional
    public PessoaResponseDTO criar(PessoaRequestDTO dto) {
        String documento = Documentos.normalizar(dto.documento());
        if (pessoaRepository.existsByDocumento(documento)) {
            throw new RegraDeNegocioException("Já existe uma pessoa cadastrada com o documento: " + documento);
        }

        PessoaModel entity = pessoaMapper.toEntity(dto);
        PessoaModel salvo = pessoaRepository.save(entity);
        PessoaResponseDTO responseDto = pessoaMapper.toResponseDTO(salvo);
        auditoriaService.registrar("Pessoa", salvo.getId(), OperacaoAuditoria.CRIACAO, null, responseDto);
        return responseDto;
    }

    /**
     * Lista completa, sem paginar. É o que alimenta os combos de pagador, beneficiário,
     * vendedor e contraparte — um select paginado esconderia pessoas válidas. A tela de
     * listagem usa {@link #buscar}.
     */
    @Transactional(readOnly = true)
    public List<PessoaResponseDTO> listarTodos() {
        return pessoaRepository.findByAtivoTrue()
                .stream()
                .map(pessoaMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<PessoaResponseDTO> buscar(String busca, boolean somenteFornecedores, int pagina, int tamanho) {
        // "Todas" aceita os dois valores do booleano; "só fornecedores" aceita apenas true.
        List<Boolean> fornecedores = somenteFornecedores ? List.of(true) : List.of(true, false);
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("nome"));
        return PaginaDTO.de(pessoaRepository.buscar(Buscas.normalizar(busca), fornecedores, pageable),
                pessoaMapper::toResponseDTO);
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
        PessoaResponseDTO estadoAnterior = pessoaMapper.toResponseDTO(entity);

        String documento = Documentos.normalizar(dto.documento());
        if (!entity.getDocumento().equalsIgnoreCase(documento)
                && pessoaRepository.existsByDocumento(documento)) {
            throw new RegraDeNegocioException("Já existe outra pessoa cadastrada com o documento: " + documento);
        }

        pessoaMapper.updateEntityFromDto(dto, entity);
        PessoaModel atualizado = pessoaRepository.save(entity);
        PessoaResponseDTO estadoNovo = pessoaMapper.toResponseDTO(atualizado);
        auditoriaService.registrar("Pessoa", id, OperacaoAuditoria.EDICAO, estadoAnterior, estadoNovo);
        return estadoNovo;
    }

    @Transactional
    public void excluir(Long id, String motivo) {
        PessoaModel entity = pessoaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada com id: " + id));
        PessoaResponseDTO estadoAnterior = pessoaMapper.toResponseDTO(entity);
        entity.getExclusao().excluir(motivo, usuarioAutenticadoService.usuarioAtual());
        PessoaModel excluido = pessoaRepository.save(entity);
        auditoriaService.registrar("Pessoa", id, OperacaoAuditoria.EXCLUSAO, estadoAnterior, pessoaMapper.toResponseDTO(excluido));
    }
}
