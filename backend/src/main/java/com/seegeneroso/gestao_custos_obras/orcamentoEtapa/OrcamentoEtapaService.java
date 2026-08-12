package com.seegeneroso.gestao_custos_obras.orcamentoEtapa;

import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.etapaProjeto.EtapaProjetoModel;
import com.seegeneroso.gestao_custos_obras.etapaProjeto.EtapaProjetoRepository;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.orcamentoEtapa.dto.OrcamentoEtapaRequestDTO;
import com.seegeneroso.gestao_custos_obras.orcamentoEtapa.dto.OrcamentoEtapaResponseDTO;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrcamentoEtapaService {

    private final OrcamentoEtapaRepository orcamentoEtapaRepository;
    private final ImovelRepository imovelRepository;
    private final EtapaProjetoRepository etapaProjetoRepository;
    private final DespesaRepository despesaRepository;
    private final OrcamentoEtapaMapper orcamentoEtapaMapper;

    @Transactional
    public OrcamentoEtapaResponseDTO criar(OrcamentoEtapaRequestDTO dto) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(dto.imovelId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + dto.imovelId()));

        EtapaProjetoModel etapa = etapaProjetoRepository.findById(dto.etapaProjetoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa de projeto não encontrada com id: " + dto.etapaProjetoId()));

        if (orcamentoEtapaRepository.existsByImovelIdAndEtapaProjetoId(dto.imovelId(), dto.etapaProjetoId())) {
            throw new RegraDeNegocioException("Já existe um orçamento cadastrado para a etapa '" + etapa.getNome() + "' neste imóvel.");
        }

        OrcamentoEtapaModel orcamento = OrcamentoEtapaModel.builder()
                .imovel(imovel)
                .etapaProjeto(etapa)
                .valorOrcado(dto.valorOrcado())
                .dataInicioPrevista(dto.dataInicioPrevista())
                .dataFimPrevista(dto.dataFimPrevista())
                .build();

        OrcamentoEtapaModel orcamentoSalvo = orcamentoEtapaRepository.save(orcamento);
        BigDecimal totalGasto = calcularTotalGasto(dto.imovelId(), dto.etapaProjetoId());

        return orcamentoEtapaMapper.toResponseDTO(orcamentoSalvo, totalGasto);
    }

    @Transactional
    public OrcamentoEtapaResponseDTO atualizar(Long id, OrcamentoEtapaRequestDTO dto) {
        OrcamentoEtapaModel orcamento = orcamentoEtapaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento de etapa não encontrado com id: " + id));

        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(dto.imovelId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + dto.imovelId()));

        EtapaProjetoModel etapa = etapaProjetoRepository.findById(dto.etapaProjetoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa de projeto não encontrada com id: " + dto.etapaProjetoId()));

        Optional<OrcamentoEtapaModel> existente = orcamentoEtapaRepository
                .findByImovelIdAndEtapaProjetoId(dto.imovelId(), dto.etapaProjetoId());

        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new RegraDeNegocioException("Já existe outro orçamento cadastrado para a etapa '" + etapa.getNome() + "' neste imóvel.");
        }

        orcamento.setImovel(imovel);
        orcamento.setEtapaProjeto(etapa);
        orcamento.setValorOrcado(dto.valorOrcado());
        orcamento.setDataInicioPrevista(dto.dataInicioPrevista());
        orcamento.setDataFimPrevista(dto.dataFimPrevista());

        OrcamentoEtapaModel orcamentoAtualizado = orcamentoEtapaRepository.save(orcamento);
        BigDecimal totalGasto = calcularTotalGasto(dto.imovelId(), dto.etapaProjetoId());

        return orcamentoEtapaMapper.toResponseDTO(orcamentoAtualizado, totalGasto);
    }

    @Transactional(readOnly = true)
    public List<OrcamentoEtapaResponseDTO> listar(Long imovelId) {
        List<OrcamentoEtapaModel> lista = imovelId != null
                ? orcamentoEtapaRepository.findByImovelId(imovelId)
                : orcamentoEtapaRepository.findAll();

        return lista.stream()
                .map(orcamento -> {
                    BigDecimal totalGasto = calcularTotalGasto(
                            orcamento.getImovel().getId(),
                            orcamento.getEtapaProjeto().getId()
                    );
                    return orcamentoEtapaMapper.toResponseDTO(orcamento, totalGasto);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OrcamentoEtapaResponseDTO buscarPorId(Long id) {
        OrcamentoEtapaModel orcamento = orcamentoEtapaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento de etapa não encontrado com id: " + id));

        BigDecimal totalGasto = calcularTotalGasto(
                orcamento.getImovel().getId(),
                orcamento.getEtapaProjeto().getId()
        );

        return orcamentoEtapaMapper.toResponseDTO(orcamento, totalGasto);
    }

    @Transactional
    public void deletar(Long id) {
        if (!orcamentoEtapaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Orçamento de etapa não encontrado com id: " + id);
        }
        orcamentoEtapaRepository.deleteById(id);
    }

    private BigDecimal calcularTotalGasto(Long imovelId, Long etapaProjetoId) {
        return despesaRepository.findByImovelIdAndEtapaProjetoId(imovelId, etapaProjetoId)
                .stream()
                .map(DespesaModel::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
