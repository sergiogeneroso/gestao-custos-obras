package com.seegeneroso.gestao_custos_obras.relatorio;

import com.seegeneroso.gestao_custos_obras.aportante.AportanteModel;
import com.seegeneroso.gestao_custos_obras.aportante.AportanteRepository;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaPagamentoModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaPagamentoRepository;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.orcamentoEtapa.OrcamentoEtapaService;
import com.seegeneroso.gestao_custos_obras.orcamentoEtapa.dto.OrcamentoEtapaResponseDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorImovelDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorM2DTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.ExtratoAportanteDTO;
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

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final ImovelRepository imovelRepository;
    private final AportanteRepository aportanteRepository;
    private final DespesaRepository despesaRepository;
    private final DespesaPagamentoRepository despesaPagamentoRepository;
    private final OrcamentoEtapaService orcamentoEtapaService;

    @Transactional(readOnly = true)
    public List<CustoPorImovelDTO> custoPorImovel(Long imovelId, Long etapaProjetoId,
                                                  LocalDate dataInicio, LocalDate dataFim) {
        List<ImovelModel> imoveis = imovelId != null
                ? List.of(buscarImovelAtivo(imovelId))
                : imovelRepository.findByAtivoTrue();

        return imoveis.stream()
                .map(imovel -> {
                    BigDecimal custoTotal = somarDespesas(imovel.getId(), etapaProjetoId, dataInicio, dataFim);
                    return new CustoPorImovelDTO(imovel.getId(), imovel.getIdentificador(), custoTotal);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CustoPorM2DTO custoPorM2(Long imovelId, Long etapaProjetoId,
                                    LocalDate dataInicio, LocalDate dataFim) {
        if (imovelId == null) {
            throw new RegraDeNegocioException("Informe o imovelId para o relatório de custo por m².");
        }
        ImovelModel imovel = buscarImovelAtivo(imovelId);
        BigDecimal custoTotal = somarDespesas(imovelId, etapaProjetoId, dataInicio, dataFim);
        BigDecimal area = imovel.getArea();
        BigDecimal custoPorM2 = (area != null && area.compareTo(BigDecimal.ZERO) > 0)
                ? custoTotal.divide(area, 2, RoundingMode.HALF_UP)
                : null;
        return new CustoPorM2DTO(imovelId, imovel.getIdentificador(), area, custoTotal, custoPorM2);
    }

    @Transactional(readOnly = true)
    public List<ExtratoAportanteDTO> extratoAportantes(Long imovelId, Long etapaProjetoId, Long aportanteId,
                                                       LocalDate dataInicio, LocalDate dataFim) {
        List<AportanteModel> aportantes = aportanteId != null
                ? List.of(buscarAportanteAtivo(aportanteId))
                : aportanteRepository.findByAtivoTrue();

        return aportantes.stream()
                .map(aportante -> {
                    BigDecimal total = despesaPagamentoRepository.findByAportanteId(aportante.getId()).stream()
                            .filter(pag -> despesaAtendeFiltros(pag.getDespesa(), imovelId, etapaProjetoId, dataInicio, dataFim))
                            .map(DespesaPagamentoModel::getValorPago)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ExtratoAportanteDTO(aportante.getId(), aportante.getNome(), total);
                })
                .filter(dto -> dto.totalAportado().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrcadoVsRealizadoDTO orcadoVsRealizado(Long imovelId) {
        if (imovelId == null) {
            throw new RegraDeNegocioException("Informe o imovelId para o relatório de orçado vs realizado.");
        }
        ImovelModel imovel = buscarImovelAtivo(imovelId);
        List<OrcamentoEtapaResponseDTO> etapas = orcamentoEtapaService.listar(imovelId);

        BigDecimal valorOrcadoTotal = etapas.stream()
                .map(OrcamentoEtapaResponseDTO::valorOrcado)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorRealizadoTotal = etapas.stream()
                .map(OrcamentoEtapaResponseDTO::totalGasto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diferenca = valorOrcadoTotal.subtract(valorRealizadoTotal);

        return new OrcadoVsRealizadoDTO(
                imovelId, imovel.getIdentificador(),
                valorOrcadoTotal, valorRealizadoTotal, diferenca, etapas
        );
    }

    private BigDecimal somarDespesas(Long imovelId, Long etapaProjetoId,
                                     LocalDate dataInicio, LocalDate dataFim) {
        return despesaRepository.findByImovelId(imovelId).stream()
                .filter(d -> despesaAtendeFiltros(d, imovelId, etapaProjetoId, dataInicio, dataFim))
                .map(DespesaModel::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean despesaAtendeFiltros(DespesaModel despesa, Long imovelId, Long etapaProjetoId,
                                         LocalDate dataInicio, LocalDate dataFim) {
        if (imovelId != null && !despesa.getImovel().getId().equals(imovelId)) {
            return false;
        }
        if (etapaProjetoId != null && !despesa.getEtapaProjeto().getId().equals(etapaProjetoId)) {
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

    private AportanteModel buscarAportanteAtivo(Long aportanteId) {
        return aportanteRepository.findByIdAndAtivoTrue(aportanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aportante não encontrado com id: " + aportanteId));
    }
}
