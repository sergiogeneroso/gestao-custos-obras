package com.seegeneroso.gestao_custos_obras.orcamentoCategoria;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaRepository;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto.OrcamentoCategoriaRequestDTO;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto.OrcamentoCategoriaResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.OperacaoAuditoria;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrcamentoCategoriaService {

    private final OrcamentoCategoriaRepository orcamentoCategoriaRepository;
    private final ImovelRepository imovelRepository;
    private final CategoriaDespesaRepository categoriaDespesaRepository;
    private final DespesaRepository despesaRepository;
    private final OrcamentoCategoriaMapper orcamentoCategoriaMapper;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public OrcamentoCategoriaResponseDTO criar(OrcamentoCategoriaRequestDTO dto) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(dto.imovelId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + dto.imovelId()));

        CategoriaDespesaModel categoria = categoriaDespesaRepository.findById(dto.categoriaDespesaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria de despesa não encontrada com id: " + dto.categoriaDespesaId()));

        if (orcamentoCategoriaRepository.existsByImovelIdAndCategoriaDespesaIdAndAtivoTrue(dto.imovelId(), dto.categoriaDespesaId())) {
            throw new RegraDeNegocioException("Já existe um orçamento cadastrado para a categoria '" + categoria.getNome() + "' neste imóvel.");
        }

        OrcamentoCategoriaModel orcamento = OrcamentoCategoriaModel.builder()
                .imovel(imovel)
                .categoriaDespesa(categoria)
                .valorOrcado(dto.valorOrcado())
                .dataInicioPrevista(dto.dataInicioPrevista())
                .dataFimPrevista(dto.dataFimPrevista())
                .build();

        OrcamentoCategoriaModel orcamentoSalvo = orcamentoCategoriaRepository.save(orcamento);
        BigDecimal totalGasto = calcularTotalGasto(dto.imovelId(), dto.categoriaDespesaId());

        OrcamentoCategoriaResponseDTO responseDto = orcamentoCategoriaMapper.toResponseDTO(orcamentoSalvo, totalGasto);
        auditoriaService.registrar("OrcamentoCategoria", orcamentoSalvo.getId(), OperacaoAuditoria.CRIACAO, null, responseDto);
        return responseDto;
    }

    @Transactional
    public OrcamentoCategoriaResponseDTO atualizar(Long id, OrcamentoCategoriaRequestDTO dto) {
        OrcamentoCategoriaModel orcamento = orcamentoCategoriaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento de categoria não encontrado com id: " + id));
        OrcamentoCategoriaResponseDTO estadoAnterior = orcamentoCategoriaMapper.toResponseDTO(orcamento,
                calcularTotalGasto(orcamento.getImovel().getId(), orcamento.getCategoriaDespesa().getId()));

        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(dto.imovelId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + dto.imovelId()));

        CategoriaDespesaModel categoria = categoriaDespesaRepository.findById(dto.categoriaDespesaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria de despesa não encontrada com id: " + dto.categoriaDespesaId()));

        Optional<OrcamentoCategoriaModel> existente = orcamentoCategoriaRepository
                .findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(dto.imovelId(), dto.categoriaDespesaId());

        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new RegraDeNegocioException("Já existe outro orçamento cadastrado para a categoria '" + categoria.getNome() + "' neste imóvel.");
        }

        orcamento.setImovel(imovel);
        orcamento.setCategoriaDespesa(categoria);
        orcamento.setValorOrcado(dto.valorOrcado());
        orcamento.setDataInicioPrevista(dto.dataInicioPrevista());
        orcamento.setDataFimPrevista(dto.dataFimPrevista());

        OrcamentoCategoriaModel orcamentoAtualizado = orcamentoCategoriaRepository.save(orcamento);
        BigDecimal totalGasto = calcularTotalGasto(dto.imovelId(), dto.categoriaDespesaId());

        OrcamentoCategoriaResponseDTO estadoNovo = orcamentoCategoriaMapper.toResponseDTO(orcamentoAtualizado, totalGasto);
        auditoriaService.registrar("OrcamentoCategoria", id, OperacaoAuditoria.EDICAO, estadoAnterior, estadoNovo);
        return estadoNovo;
    }

    @Transactional(readOnly = true)
    public List<OrcamentoCategoriaResponseDTO> listar(Long imovelId) {
        List<OrcamentoCategoriaModel> lista = imovelId != null
                ? orcamentoCategoriaRepository.findByImovelId(imovelId)
                : orcamentoCategoriaRepository.findAll().stream()
                        .filter(o -> Boolean.TRUE.equals(o.getExclusao().getAtivo()))
                        .toList();

        return lista.stream()
                .map(orcamento -> {
                    BigDecimal totalGasto = calcularTotalGasto(
                            orcamento.getImovel().getId(),
                            orcamento.getCategoriaDespesa().getId()
                    );
                    return orcamentoCategoriaMapper.toResponseDTO(orcamento, totalGasto);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrcamentoCategoriaResponseDTO buscarPorId(Long id) {
        OrcamentoCategoriaModel orcamento = orcamentoCategoriaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento de categoria não encontrado com id: " + id));

        BigDecimal totalGasto = calcularTotalGasto(
                orcamento.getImovel().getId(),
                orcamento.getCategoriaDespesa().getId()
        );

        return orcamentoCategoriaMapper.toResponseDTO(orcamento, totalGasto);
    }

    @Transactional
    public void excluir(Long id, String motivo) {
        OrcamentoCategoriaModel orcamento = orcamentoCategoriaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento de categoria não encontrado com id: " + id));
        BigDecimal totalGasto = calcularTotalGasto(orcamento.getImovel().getId(), orcamento.getCategoriaDespesa().getId());
        OrcamentoCategoriaResponseDTO estadoAnterior = orcamentoCategoriaMapper.toResponseDTO(orcamento, totalGasto);
        orcamento.getExclusao().excluir(motivo, usuarioAutenticadoService.usuarioAtual());
        OrcamentoCategoriaModel excluido = orcamentoCategoriaRepository.save(orcamento);
        auditoriaService.registrar("OrcamentoCategoria", id, OperacaoAuditoria.EXCLUSAO, estadoAnterior,
                orcamentoCategoriaMapper.toResponseDTO(excluido, totalGasto));
    }

    private BigDecimal calcularTotalGasto(Long imovelId, Long categoriaDespesaId) {
        return despesaRepository.findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(imovelId, categoriaDespesaId)
                .stream()
                .map(DespesaModel::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
