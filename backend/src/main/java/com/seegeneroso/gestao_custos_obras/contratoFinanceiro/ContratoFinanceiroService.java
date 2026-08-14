package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroResponseDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoQuitacaoRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ParcelaContratoRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ParcelaPagamentoRequestDTO;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoFinanceiroService {

    private final ContratoFinanceiroRepository contratoFinanceiroRepository;
    private final ParcelaContratoRepository parcelaContratoRepository;
    private final ImovelRepository imovelRepository;
    private final PessoaRepository pessoaRepository;
    private final ContratoFinanceiroMapper contratoFinanceiroMapper;

    @Transactional
    public ContratoFinanceiroResponseDTO criar(ContratoFinanceiroRequestDTO dto) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(dto.imovelId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + dto.imovelId()));
        PessoaModel contraparte = pessoaRepository.findByIdAndAtivoTrue(dto.contraparteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contraparte não encontrada com id: " + dto.contraparteId()));

        ContratoFinanceiroModel contrato = ContratoFinanceiroModel.builder()
                .imovel(imovel)
                .tipo(dto.tipo())
                .contraparte(contraparte)
                .valorContratado(dto.valorContratado())
                .parcelas(new ArrayList<>())
                .build();

        if (dto.parcelas() != null) {
            for (ParcelaContratoRequestDTO parcelaDto : dto.parcelas()) {
                contrato.getParcelas().add(ParcelaContratoModel.builder()
                        .contrato(contrato)
                        .numero(parcelaDto.numero())
                        .dataVencimento(parcelaDto.dataVencimento())
                        .valor(parcelaDto.valor())
                        .valorJuros(parcelaDto.valorJuros())
                        .build());
            }
        }

        ContratoFinanceiroModel salvo = contratoFinanceiroRepository.save(contrato);
        return contratoFinanceiroMapper.toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<ContratoFinanceiroResponseDTO> listar(Long imovelId) {
        List<ContratoFinanceiroModel> contratos = imovelId != null
                ? contratoFinanceiroRepository.findByImovelId(imovelId)
                : contratoFinanceiroRepository.findAll();
        return contratos.stream().map(contratoFinanceiroMapper::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public ContratoFinanceiroResponseDTO buscarPorId(Long id) {
        return contratoFinanceiroMapper.toResponseDTO(buscarContrato(id));
    }

    @Transactional
    public ContratoFinanceiroResponseDTO quitar(Long id, ContratoQuitacaoRequestDTO dto) {
        ContratoFinanceiroModel contrato = buscarContrato(id);

        if (contrato.getSituacao() == SituacaoContrato.QUITADO) {
            throw new RegraDeNegocioException("Contrato já está quitado.");
        }

        contrato.setSituacao(SituacaoContrato.QUITADO);
        contrato.setDataQuitacao(dto.dataQuitacao());
        contrato.setValorQuitacao(dto.valorQuitacao());

        ContratoFinanceiroModel atualizado = contratoFinanceiroRepository.save(contrato);
        return contratoFinanceiroMapper.toResponseDTO(atualizado);
    }

    @Transactional
    public ContratoFinanceiroResponseDTO pagarParcela(Long contratoId, Long parcelaId, ParcelaPagamentoRequestDTO dto) {
        ContratoFinanceiroModel contrato = buscarContrato(contratoId);

        ParcelaContratoModel parcela = parcelaContratoRepository.findById(parcelaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Parcela não encontrada com id: " + parcelaId));

        if (!parcela.getContrato().getId().equals(contratoId)) {
            throw new RegraDeNegocioException("A parcela não pertence ao contrato informado.");
        }

        parcela.setDataPagamento(dto.dataPagamento());
        parcela.setValorPago(dto.valorPago());
        parcelaContratoRepository.save(parcela);

        return contratoFinanceiroMapper.toResponseDTO(contrato);
    }

    private ContratoFinanceiroModel buscarContrato(Long id) {
        return contratoFinanceiroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato financeiro não encontrado com id: " + id));
    }
}
