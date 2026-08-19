package com.seegeneroso.gestao_custos_obras.relatorio;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ParcelaContratoModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.OrcamentoCategoriaService;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.dto.OrcamentoCategoriaResponseDTO;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CarteiraDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorImovelDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.CustoPorM2DTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.ExtratoPessoaDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.OrcadoVsRealizadoDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.PosicaoContratoDTO;
import com.seegeneroso.gestao_custos_obras.relatorio.dto.ResultadoImovelDTO;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final ImovelRepository imovelRepository;
    private final PessoaRepository pessoaRepository;
    private final DespesaRepository despesaRepository;
    private final ContratoFinanceiroRepository contratoFinanceiroRepository;
    private final OrcamentoCategoriaService orcamentoCategoriaService;

    @Transactional(readOnly = true)
    public List<CustoPorImovelDTO> custoPorImovel(Long imovelId, Long categoriaDespesaId,
                                                  LocalDate dataInicio, LocalDate dataFim) {
        List<ImovelModel> imoveis = imovelId != null
                ? List.of(buscarImovelAtivo(imovelId))
                : imovelRepository.findByAtivoTrue();

        return imoveis.stream()
                .map(imovel -> {
                    BigDecimal custoTotal = somarDespesas(imovel.getId(), categoriaDespesaId, dataInicio, dataFim, null);
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
        BigDecimal custoTotal = somarDespesas(imovelId, categoriaDespesaId, dataInicio, dataFim, null);
        BigDecimal custoObra = somarDespesas(imovelId, categoriaDespesaId, dataInicio, dataFim, FaseImovel.CONSTRUCAO);

        // Dois indicadores porque as metragens medem coisas diferentes (ADR-030): o custo do imóvel
        // sobre a área do lote, e o custo da obra sobre a área construída — este último é o que
        // compara uma construção com outra.
        BigDecimal custoPorM2 = dividirPorArea(custoTotal, imovel.getLote().getArea());
        BigDecimal custoObraPorM2 = dividirPorArea(custoObra, imovel.getConstrucao().getArea());

        return new CustoPorM2DTO(imovelId, imovel.getIdentificador(), imovel.getLote().getArea(),
                imovel.getConstrucao().getArea(), custoTotal, custoPorM2, custoObra, custoObraPorM2);
    }

    private BigDecimal dividirPorArea(BigDecimal valor, BigDecimal area) {
        return (area != null && area.compareTo(BigDecimal.ZERO) > 0)
                ? valor.divide(area, 2, RoundingMode.HALF_UP)
                : null;
    }

    @Transactional(readOnly = true)
    public List<ExtratoPessoaDTO> extratoPessoas(Long imovelId, Long categoriaDespesaId, Long pessoaId,
                                                 LocalDate dataInicio, LocalDate dataFim) {
        return extratoPorPapel(DespesaModel::getPagador, pessoaId, imovelId, categoriaDespesaId, dataInicio, dataFim);
    }

    @Transactional(readOnly = true)
    public List<ExtratoPessoaDTO> historicoFornecedor(Long imovelId, Long categoriaDespesaId, Long pessoaId,
                                                       LocalDate dataInicio, LocalDate dataFim) {
        return extratoPorPapel(DespesaModel::getBeneficiario, pessoaId, imovelId, categoriaDespesaId, dataInicio, dataFim);
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

    // A regra de custo (ADR-025, .agents/rules/regras-negocio-financeiras.md e contratos-financeiros.md):
    // custoTotal = valor de compra + despesas do imóvel (todas as fases) + juros efetivamente pagos nas
    // parcelas. Prestação de contrato NUNCA é despesa e saldo devedor/valorQuitacao NUNCA entram aqui —
    // são posição de caixa, expostos à parte em PosicaoContratoDTO. Gasto geral (despesa sem imóvel) não
    // entra no custo de imóvel nenhum porque nunca é buscado por findByImovelIdAndAtivoTrue.
    @Transactional(readOnly = true)
    public ResultadoImovelDTO resultadoImovel(Long imovelId) {
        ImovelModel imovel = buscarImovelAtivo(imovelId);
        List<DespesaModel> despesas = despesaRepository.findByImovelIdAndAtivoTrue(imovelId);
        List<ContratoFinanceiroModel> contratos = contratoFinanceiroRepository.findByImovelId(imovelId);

        Map<FaseImovel, BigDecimal> despesasPorFase = despesas.stream()
                .collect(Collectors.groupingBy(DespesaModel::getFaseImovel,
                        Collectors.reducing(BigDecimal.ZERO, DespesaModel::getValor, BigDecimal::add)));
        BigDecimal totalDespesas = despesas.stream().map(DespesaModel::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal jurosPagos = jurosPagos(contratos);
        BigDecimal custoTotal = custoTotal(imovel, totalDespesas, jurosPagos);

        boolean vendido = imovel.getSituacao() == SituacaoImovel.VENDIDO;
        LocalDate fimCarteira = vendido && imovel.getVenda().getData() != null
                ? imovel.getVenda().getData() : LocalDate.now();
        long diasEmCarteira = ChronoUnit.DAYS.between(imovel.getCompra().getData(), fimCarteira);

        BigDecimal lucro = null;
        BigDecimal margem = null;
        if (vendido && imovel.getVenda().getValor() != null) {
            lucro = imovel.getVenda().getValor().subtract(custoTotal);
            if (imovel.getVenda().getValor().compareTo(BigDecimal.ZERO) > 0) {
                margem = lucro.divide(imovel.getVenda().getValor(), 4, RoundingMode.HALF_UP);
            }
        }

        // rentabilidadeAnualizada é indicador percentual, não valor monetário — exceção documentada
        // à proibição de double de .agents/rules/regras-negocio-financeiras.md.
        Double rentabilidadeAnualizada = null;
        if (lucro != null && diasEmCarteira > 0 && custoTotal.compareTo(BigDecimal.ZERO) > 0) {
            double roi = lucro.divide(custoTotal, 10, RoundingMode.HALF_UP).doubleValue();
            rentabilidadeAnualizada = Math.pow(1 + roi, 365.0 / diasEmCarteira) - 1;
        }

        boolean resultadoProvisorio = vendido && imovel.getFase() != FaseImovel.CASA;

        return new ResultadoImovelDTO(
                imovel.getId(), imovel.getIdentificador(), imovel.getFase(), imovel.getSituacao(),
                imovel.getCompra().getValor(),
                despesasPorFase, totalDespesas, jurosPagos, custoTotal,
                imovel.getConstrucao().getCustoEstimado(), imovel.getConstrucao().getPrevisaoConclusao(),
                despesasPorFase.getOrDefault(FaseImovel.CONSTRUCAO, BigDecimal.ZERO),
                imovel.getVenda().getValor(), imovel.getVenda().getValorPretendido(), imovel.getVenda().getData(),
                lucro, margem, diasEmCarteira, tempoPorFase(imovel), rentabilidadeAnualizada, resultadoProvisorio,
                contratos.stream().map(this::posicaoContrato).toList()
        );
    }

    @Transactional(readOnly = true)
    public CarteiraDTO carteira(LocalDate dataInicio, LocalDate dataFim) {
        List<ImovelModel> imoveis = imovelRepository.findByAtivoTrue();
        LocalDate hoje = LocalDate.now();
        LocalDate limite30 = hoje.plusDays(30);

        BigDecimal totalInvestido = BigDecimal.ZERO;
        BigDecimal totalVendido = BigDecimal.ZERO;
        BigDecimal lucroRealizado = BigDecimal.ZERO;
        BigDecimal saldoDevedorTotal = BigDecimal.ZERO;
        BigDecimal saldoAReceberTotal = BigDecimal.ZERO;
        long parcelasAVencer = 0;
        long parcelasAReceber = 0;
        Map<FaseImovel, Long> imoveisPorFase = new EnumMap<>(FaseImovel.class);
        Map<SituacaoImovel, Long> imoveisPorSituacao = new EnumMap<>(SituacaoImovel.class);

        for (ImovelModel imovel : imoveis) {
            imoveisPorFase.merge(imovel.getFase(), 1L, Long::sum);
            imoveisPorSituacao.merge(imovel.getSituacao(), 1L, Long::sum);

            List<DespesaModel> despesas = despesaRepository.findByImovelIdAndAtivoTrue(imovel.getId());
            BigDecimal totalDespesas = despesas.stream().map(DespesaModel::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<ContratoFinanceiroModel> contratos = contratoFinanceiroRepository.findByImovelId(imovel.getId());
            BigDecimal custoTotal = custoTotal(imovel, totalDespesas, jurosPagos(contratos));
            totalInvestido = totalInvestido.add(custoTotal);

            if (imovel.getSituacao() == SituacaoImovel.VENDIDO && imovel.getVenda().getValor() != null) {
                totalVendido = totalVendido.add(imovel.getVenda().getValor());
                lucroRealizado = lucroRealizado.add(imovel.getVenda().getValor().subtract(custoTotal));
            }

            for (ContratoFinanceiroModel contrato : contratos) {
                if (ehDivida(contrato)) {
                    saldoDevedorTotal = saldoDevedorTotal.add(saldoEmAberto(contrato));
                    parcelasAVencer += contarParcelasEmAberto(contrato, hoje, limite30);
                } else {
                    saldoAReceberTotal = saldoAReceberTotal.add(saldoEmAberto(contrato));
                    parcelasAReceber += contarParcelasEmAberto(contrato, hoje, limite30);
                }
            }
        }

        BigDecimal gastosGeraisPeriodo = despesaRepository.findByImovelIsNullAndAtivoTrue().stream()
                .filter(d -> despesaAtendeFiltros(d, null, null, dataInicio, dataFim))
                .map(DespesaModel::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarteiraDTO(totalInvestido, totalVendido, lucroRealizado, imoveisPorFase, imoveisPorSituacao,
                saldoDevedorTotal, saldoAReceberTotal, parcelasAVencer, parcelasAReceber, gastosGeraisPeriodo);
    }

    // O tipo do contrato decide de que lado ele conta: PARCELAMENTO_VENDA é crédito contra o
    // comprador, não dívida do imóvel — os juros que ele paga não são custo, e o que ele ainda
    // deve é "a receber", nunca saldo devedor. Ver .agents/rules/contratos-financeiros.md.
    private boolean ehDivida(ContratoFinanceiroModel contrato) {
        return contrato.getTipo() != TipoContratoFinanceiro.PARCELAMENTO_VENDA;
    }

    private BigDecimal jurosPagos(List<ContratoFinanceiroModel> contratos) {
        return contratos.stream()
                .filter(this::ehDivida)
                .flatMap(c -> c.getParcelas().stream())
                .filter(p -> p.getDataPagamento() != null)
                .map(ParcelaContratoModel::getValorJuros)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal custoTotal(ImovelModel imovel, BigDecimal totalDespesas, BigDecimal jurosPagos) {
        BigDecimal valorCompra = imovel.getCompra().getValor() != null ? imovel.getCompra().getValor() : BigDecimal.ZERO;
        return valorCompra.add(totalDespesas).add(jurosPagos);
    }

    // Soma das parcelas ainda não baixadas — a pagar num contrato de dívida, a receber num
    // PARCELAMENTO_VENDA. Quem chama é que decide de que lado somar (ver ehDivida).
    private BigDecimal saldoEmAberto(ContratoFinanceiroModel contrato) {
        if (contrato.getSituacao() == SituacaoContrato.QUITADO) {
            return BigDecimal.ZERO;
        }
        return contrato.getParcelas().stream()
                .filter(p -> p.getDataPagamento() == null)
                .map(ParcelaContratoModel::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long contarParcelasEmAberto(ContratoFinanceiroModel contrato, LocalDate hoje, LocalDate limite) {
        if (contrato.getSituacao() != SituacaoContrato.ATIVO) {
            return 0;
        }
        return contrato.getParcelas().stream()
                .filter(p -> p.getDataPagamento() == null)
                .filter(p -> !p.getDataVencimento().isBefore(hoje) && !p.getDataVencimento().isAfter(limite))
                .count();
    }

    private PosicaoContratoDTO posicaoContrato(ContratoFinanceiroModel contrato) {
        BigDecimal totalPagoParcelas = contrato.getParcelas().stream()
                .filter(p -> p.getDataPagamento() != null)
                .map(ParcelaContratoModel::getValorPago)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean quitado = contrato.getSituacao() == SituacaoContrato.QUITADO;
        BigDecimal totalPago = quitado && contrato.getValorQuitacao() != null
                ? totalPagoParcelas.add(contrato.getValorQuitacao())
                : totalPagoParcelas;

        return new PosicaoContratoDTO(
                contrato.getId(), contrato.getTipo(),
                contrato.getContraparte() != null ? contrato.getContraparte().getNome() : null,
                contrato.getSituacao(), contrato.getValorContratado(), totalPago, saldoEmAberto(contrato));
    }

    private Map<FaseImovel, Long> tempoPorFase(ImovelModel imovel) {
        LocalDate hoje = LocalDate.now();
        Map<FaseImovel, Long> tempo = new EnumMap<>(FaseImovel.class);

        LocalDate fimLote = imovel.getConstrucao().getDataInicio() != null ? imovel.getConstrucao().getDataInicio() : hoje;
        tempo.put(FaseImovel.LOTE, ChronoUnit.DAYS.between(imovel.getCompra().getData(), fimLote));

        if (imovel.getConstrucao().getDataInicio() != null) {
            LocalDate fimConstrucao = imovel.getCasa().getDataConclusaoObra() != null ? imovel.getCasa().getDataConclusaoObra() : hoje;
            tempo.put(FaseImovel.CONSTRUCAO, ChronoUnit.DAYS.between(imovel.getConstrucao().getDataInicio(), fimConstrucao));
        }

        if (imovel.getCasa().getDataConclusaoObra() != null) {
            boolean vendido = imovel.getSituacao() == SituacaoImovel.VENDIDO && imovel.getVenda().getData() != null;
            LocalDate fimCasa = vendido ? imovel.getVenda().getData() : hoje;
            tempo.put(FaseImovel.CASA, ChronoUnit.DAYS.between(imovel.getCasa().getDataConclusaoObra(), fimCasa));
        }

        return tempo;
    }

    private List<ExtratoPessoaDTO> extratoPorPapel(Function<DespesaModel, PessoaModel> papel, Long pessoaId,
                                                    Long imovelId, Long categoriaDespesaId,
                                                    LocalDate dataInicio, LocalDate dataFim) {
        List<PessoaModel> pessoas = pessoaId != null
                ? List.of(buscarPessoaAtiva(pessoaId))
                : pessoaRepository.findByAtivoTrue();
        List<DespesaModel> despesasAtivas = despesaRepository.findByAtivoTrue();

        return pessoas.stream()
                .map(pessoa -> {
                    BigDecimal total = despesasAtivas.stream()
                            .filter(d -> papel.apply(d) != null && papel.apply(d).getId().equals(pessoa.getId()))
                            .filter(d -> despesaAtendeFiltros(d, imovelId, categoriaDespesaId, dataInicio, dataFim))
                            .map(DespesaModel::getValor)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ExtratoPessoaDTO(pessoa.getId(), pessoa.getNome(), total);
                })
                .filter(dto -> dto.totalPago().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    // fase nula = todas as fases.
    private BigDecimal somarDespesas(Long imovelId, Long categoriaDespesaId,
                                     LocalDate dataInicio, LocalDate dataFim, FaseImovel fase) {
        return despesaRepository.findByImovelIdAndAtivoTrue(imovelId).stream()
                .filter(d -> despesaAtendeFiltros(d, imovelId, categoriaDespesaId, dataInicio, dataFim))
                .filter(d -> fase == null || fase == d.getFaseImovel())
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
