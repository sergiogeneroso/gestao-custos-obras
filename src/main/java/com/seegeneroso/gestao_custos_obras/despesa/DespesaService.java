package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.aportante.AportanteModel;
import com.seegeneroso.gestao_custos_obras.aportante.AportanteRepository;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaPagamentoRequestDTO;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaRequestDTO;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaResponseDTO;
import com.seegeneroso.gestao_custos_obras.etapaProjeto.EtapaProjetoModel;
import com.seegeneroso.gestao_custos_obras.etapaProjeto.EtapaProjetoRepository;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final ImovelRepository imovelRepository;
    private final EtapaProjetoRepository etapaProjetoRepository;
    private final AportanteRepository aportanteRepository;
    private final com.seegeneroso.gestao_custos_obras.shared.storage.StorageService storageService;
    private final DespesaMapper despesaMapper;

    @Transactional
    public DespesaResponseDTO criar(DespesaRequestDTO dto) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(dto.imovelId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + dto.imovelId()));

        EtapaProjetoModel etapa = etapaProjetoRepository.findById(dto.etapaProjetoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa de projeto não encontrada com id: " + dto.etapaProjetoId()));

        DespesaModel despesa = DespesaModel.builder()
                .imovel(imovel)
                .etapaProjeto(etapa)
                .valor(dto.valor())
                .dataPagamento(dto.dataPagamento())
                .descricao(dto.descricao())
                .comprovanteUrl(dto.comprovanteUrl())
                .pagamentos(new ArrayList<>())
                .build();

        if (dto.pagamentos() != null && !dto.pagamentos().isEmpty()) {
            BigDecimal somaPagamentos = BigDecimal.ZERO;

            for (DespesaPagamentoRequestDTO pagDto : dto.pagamentos()) {
                AportanteModel aportante = aportanteRepository.findByIdAndAtivoTrue(pagDto.aportanteId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Aportante não encontrado com id: " + pagDto.aportanteId()));

                somaPagamentos = somaPagamentos.add(pagDto.valorPago());

                DespesaPagamentoModel pagamento = DespesaPagamentoModel.builder()
                        .despesa(despesa)
                        .aportante(aportante)
                        .valorPago(pagDto.valorPago())
                        .build();

                despesa.getPagamentos().add(pagamento);
            }

            if (somaPagamentos.compareTo(dto.valor()) > 0) {
                throw new RegraDeNegocioException("A soma dos pagamentos (" + somaPagamentos + ") não pode exceder o valor total da despesa (" + dto.valor() + ")");
            }
        }

        DespesaModel despesaSalva = despesaRepository.save(despesa);
        return despesaMapper.toResponseDTO(despesaSalva);
    }

    @Transactional
    public DespesaResponseDTO atualizar(Long id, DespesaRequestDTO dto) {
        DespesaModel despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada com id: " + id));

        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(dto.imovelId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + dto.imovelId()));

        EtapaProjetoModel etapa = etapaProjetoRepository.findById(dto.etapaProjetoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa de projeto não encontrada com id: " + dto.etapaProjetoId()));

        despesa.setImovel(imovel);
        despesa.setEtapaProjeto(etapa);
        despesa.setValor(dto.valor());
        despesa.setDataPagamento(dto.dataPagamento());
        despesa.setDescricao(dto.descricao());
        despesa.setComprovanteUrl(dto.comprovanteUrl());

        despesa.getPagamentos().clear();

        if (dto.pagamentos() != null && !dto.pagamentos().isEmpty()) {
            BigDecimal somaPagamentos = BigDecimal.ZERO;

            for (DespesaPagamentoRequestDTO pagDto : dto.pagamentos()) {
                AportanteModel aportante = aportanteRepository.findByIdAndAtivoTrue(pagDto.aportanteId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Aportante não encontrado com id: " + pagDto.aportanteId()));

                somaPagamentos = somaPagamentos.add(pagDto.valorPago());

                DespesaPagamentoModel pagamento = DespesaPagamentoModel.builder()
                        .despesa(despesa)
                        .aportante(aportante)
                        .valorPago(pagDto.valorPago())
                        .build();

                despesa.getPagamentos().add(pagamento);
            }

            if (somaPagamentos.compareTo(dto.valor()) > 0) {
                throw new RegraDeNegocioException("A soma dos pagamentos (" + somaPagamentos + ") não pode exceder o valor total da despesa (" + dto.valor() + ")");
            }
        }

        DespesaModel despesaAtualizada = despesaRepository.save(despesa);
        return despesaMapper.toResponseDTO(despesaAtualizada);
    }

    @Transactional(readOnly = true)
    public List<DespesaResponseDTO> listar(Long imovelId, Long etapaProjetoId) {
        List<DespesaModel> despesas;

        if (imovelId != null && etapaProjetoId != null) {
            despesas = despesaRepository.findByImovelIdAndEtapaProjetoId(imovelId, etapaProjetoId);
        } else if (imovelId != null) {
            despesas = despesaRepository.findByImovelId(imovelId);
        } else if (etapaProjetoId != null) {
            despesas = despesaRepository.findByEtapaProjetoId(etapaProjetoId);
        } else {
            despesas = despesaRepository.findAll();
        }

        return despesas.stream()
                .map(despesaMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DespesaResponseDTO> listarTodas() {
        return listar(null, null);
    }

    @Transactional(readOnly = true)
    public List<DespesaResponseDTO> listarPorImovel(Long imovelId) {
        if (!imovelRepository.existsById(imovelId)) {
            throw new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId);
        }
        return listar(imovelId, null);
    }

    @Transactional(readOnly = true)
    public DespesaResponseDTO buscarPorId(Long id) {
        DespesaModel despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada com id: " + id));
        return despesaMapper.toResponseDTO(despesa);
    }

    @Transactional
    public void deletar(Long id) {
        if (!despesaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Despesa não encontrada com id: " + id);
        }
        despesaRepository.deleteById(id);
    }

    @Transactional
    public DespesaResponseDTO uploadComprovante(Long id, org.springframework.web.multipart.MultipartFile arquivo) {
        DespesaModel despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada com id: " + id));

        String subpasta = "despesas/" + id;
        String nomeArquivo = storageService.salvar(arquivo, subpasta);

        String fileUri = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/arquivos/download/")
                .path(subpasta + "/")
                .path(nomeArquivo)
                .toUriString();

        despesa.setComprovanteUrl(fileUri);
        DespesaModel despesaSalva = despesaRepository.save(despesa);
        return despesaMapper.toResponseDTO(despesaSalva);
    }
}
