package com.seegeneroso.gestao_custos_obras.relatorio;

import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.OrcamentoCategoriaService;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto.OrcamentoCategoriaResponseDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorImovelDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorM2DTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.ExtratoPessoaDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.OrcadoVsRealizadoDTO;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

// Adaptação mínima para compilar após o reescopo de Ago 2026 (ADR-019 a ADR-029).
// A reescrita completa (juros, custos acessórios de contrato, resultado por imóvel) é a Etapa F.
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final ImovelRepository imovelRepository;
    private final PessoaRepository pessoaRepository;
    private final DespesaRepository despesaRepository;
    private final OrcamentoCategoriaService orcamentoCategoriaService;

    @Transactional(readOnly = true)
    public List<CustoPorImovelDTO> custoPorImovel(Long imovelId, Long categoriaDespesaId,
                                                  LocalDate dataInicio, LocalDate dataFim) {
        List<ImovelModel> imoveis = imovelId != null
                ? List.of(buscarImovelAtivo(imovelId))
                : imovelRepository.findByAtivoTrue();

        return imoveis.stream()
                .map(imovel -> {
                    BigDecimal custoTotal = somarDespesas(imovel.getId(), categoriaDespesaId, dataInicio, dataFim);
                    return new CustoPorImovelDTO(imovel.getId(), imovel.getIdentificador(), custoTotal);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CustoPorM2DTO custoPorM2(Long imovelId, Long categoriaDespesaId,
                                    LocalDate dataInicio, LocalDate dataFim) {
        if (imovelId == null) {
            throw new RegraDeNegocioException("Informe o imovelId para o relatório de custo por m².");
        }
        ImovelModel imovel = buscarImovelAtivo(imovelId);
        BigDecimal custoTotal = somarDespesas(imovelId, categoriaDespesaId, dataInicio, dataFim);
        BigDecimal area = imovel.getArea();
        BigDecimal custoPorM2 = (area != null && area.compareTo(BigDecimal.ZERO) > 0)
                ? custoTotal.divide(area, 2, RoundingMode.HALF_UP)
                : null;
        return new CustoPorM2DTO(imovelId, imovel.getIdentificador(), area, custoTotal, custoPorM2);
    }

    @Transactional(readOnly = true)
    public List<ExtratoPessoaDTO> extratoPessoas(Long imovelId, Long categoriaDespesaId, Long pessoaId,
                                                 LocalDate dataInicio, LocalDate dataFim) {
        List<PessoaModel> pessoas = pessoaId != null
                ? List.of(buscarPessoaAtiva(pessoaId))
                : pessoaRepository.findByAtivoTrue();

        return pessoas.stream()
                .map(pessoa -> {
                    BigDecimal total = despesaRepository.findByAtivoTrue().stream()
                            .filter(d -> d.getPagador() != null && d.getPagador().getId().equals(pessoa.getId()))
                            .filter(d -> despesaAtendeFiltros(d, imovelId, categoriaDespesaId, dataInicio, dataFim))
                            .map(DespesaModel::getValor)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ExtratoPessoaDTO(pessoa.getId(), pessoa.getNome(), total);
                })
                .filter(dto -> dto.totalPago().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrcadoVsRealizadoDTO orcadoVsRealizado(Long imovelId) {
        if (imovelId == null) {
            throw new RegraDeNegocioException("Informe o imovelId para o relatório de orçado vs realizado.");
        }
        ImovelModel imovel = buscarImovelAtivo(imovelId);
        List<OrcamentoCategoriaResponseDTO> categorias = orcamentoCategoriaService.listar(imovelId);

        BigDecimal valorOrcadoTotal = categorias.stream()
                .map(OrcamentoCategoriaResponseDTO::valorOrcado)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorRealizadoTotal = categorias.stream()
                .map(OrcamentoCategoriaResponseDTO::totalGasto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diferenca = valorOrcadoTotal.subtract(valorRealizadoTotal);

        return new OrcadoVsRealizadoDTO(
                imovelId, imovel.getIdentificador(),
                valorOrcadoTotal, valorRealizadoTotal, diferenca, categorias
        );
    }

    private BigDecimal somarDespesas(Long imovelId, Long categoriaDespesaId,
                                     LocalDate dataInicio, LocalDate dataFim) {
        return despesaRepository.findByImovelIdAndAtivoTrue(imovelId).stream()
                .filter(d -> despesaAtendeFiltros(d, imovelId, categoriaDespesaId, dataInicio, dataFim))
                .map(DespesaModel::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean despesaAtendeFiltros(DespesaModel despesa, Long imovelId, Long categoriaDespesaId,
                                         LocalDate dataInicio, LocalDate dataFim) {
        if (imovelId != null && (despesa.getImovel() == null || !despesa.getImovel().getId().equals(imovelId))) {
            return false;
        }
        if (categoriaDespesaId != null && !despesa.getCategoriaDespesa().getId().equals(categoriaDespesaId)) {
            return false;
        }
        if (dataInicio != null && despesa.getDataPagamento().isBefore(dataInicio)) {
            return false;
        }
        if (dataFim != null && despesa.getDataPagamento().isAfter(dataFim)) {
            return false;
        }
        return true;
    }

    private ImovelModel buscarImovelAtivo(Long imovelId) {
        return imovelRepository.findByIdAndAtivoTrue(imovelId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId));
    }

    private PessoaModel buscarPessoaAtiva(Long pessoaId) {
        return pessoaRepository.findByIdAndAtivoTrue(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada com id: " + pessoaId));
    }
}
