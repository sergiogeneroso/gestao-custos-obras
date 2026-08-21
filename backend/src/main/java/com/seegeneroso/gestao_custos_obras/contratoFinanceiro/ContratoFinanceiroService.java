package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoDocumentoResponseDTO;
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
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoDocumentoContrato;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import com.seegeneroso.gestao_custos_obras.shared.storage.ArquivoUrls;
import com.seegeneroso.gestao_custos_obras.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoFinanceiroService {

    private final ContratoFinanceiroRepository contratoFinanceiroRepository;
    private final ParcelaContratoRepository parcelaContratoRepository;
    private final ImovelRepository imovelRepository;
    private final PessoaRepository pessoaRepository;
    private final ContratoDocumentoRepository contratoDocumentoRepository;
    private final StorageService storageService;
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

        if (dto.entradaValor() != null && dto.entradaValor().compareTo(BigDecimal.ZERO) > 0) {
            contrato.getParcelas().add(montarEntrada(contrato, dto));
        }

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
        aplicarValorDoLote(imovel, salvo, dto);
        return contratoFinanceiroMapper.toResponseDTO(salvo);
    }

    // A entrada é fato consumado no momento da compra, não evento futuro: nasce como parcela nº 0 já
    // baixada, reaproveitando total pago e saldo devedor sem caso especial (ADR-037).
    private ParcelaContratoModel montarEntrada(ContratoFinanceiroModel contrato, ContratoFinanceiroRequestDTO dto) {
        LocalDate data = dto.entradaData() != null ? dto.entradaData() : contrato.getImovel().getCompra().getData();
        return ParcelaContratoModel.builder()
                .contrato(contrato)
                .numero(0)
                .dataVencimento(data)
                .valor(dto.entradaValor())
                .dataPagamento(data)
                .valorPago(dto.entradaValor())
                .build();
    }

    /**
     * Na compra parcelada o formulário do imóvel não pede o valor do lote, justamente para não pedir
     * um número que precisa espelhar um cronograma que ainda não existe (ADR-037). Quem grava é aqui:
     * o preço à vista informado, ou o total do cronograma quando não houver — que é o caso normal,
     * porque o parcelamento do lote costuma ser sem juros.
     *
     * Só na criação, e só se estiver vazio: reescrever o valor ao editar o cronograma mudaria em
     * silêncio o custo de um imóvel já apurado.
     */
    private void aplicarValorDoLote(ImovelModel imovel, ContratoFinanceiroModel contrato,
                                    ContratoFinanceiroRequestDTO dto) {
        if (contrato.getTipo() != TipoContratoFinanceiro.PARCELAMENTO_COMPRA
                || imovel.getCompra().getValor() != null) {
            return;
        }

        BigDecimal valor = dto.precoAVistaLote() != null
                ? dto.precoAVistaLote()
                : contrato.getParcelas().stream()
                        .map(ParcelaContratoModel::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        imovel.getCompra().setValor(valor);
        imovelRepository.save(imovel);
    }

    /**
     * Edição do contrato. Duas coisas não podem ser reescritas, por ADR-025:
     * - contrato QUITADO é histórico fechado (a quitação tem valor próprio, negociado, e as parcelas
     *   originais precisam continuar legíveis);
     * - parcela já paga não pode ter valor, juros, vencimento ou número alterados, nem ser removida,
     *   porque o valorJuros dela já entrou em jurosPagos/custoTotal do relatório.
     * Não validar a soma das parcelas contra valorContratado: juros fazem a soma exceder o principal
     * legitimamente.
     */
    @Transactional
    public ContratoFinanceiroResponseDTO atualizar(Long id, ContratoFinanceiroRequestDTO dto) {
        ContratoFinanceiroModel contrato = buscarContrato(id);

        if (contrato.getSituacao() == SituacaoContrato.QUITADO) {
            throw new RegraDeNegocioException("Contrato quitado não pode ser editado.");
        }

        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(dto.imovelId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + dto.imovelId()));
        PessoaModel contraparte = pessoaRepository.findByIdAndAtivoTrue(dto.contraparteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contraparte não encontrada com id: " + dto.contraparteId()));

        contrato.setImovel(imovel);
        contrato.setTipo(dto.tipo());
        contrato.setContraparte(contraparte);
        contrato.setValorContratado(dto.valorContratado());

        aplicarParcelas(contrato, dto.parcelas() != null ? dto.parcelas() : List.of());

        ContratoFinanceiroModel atualizado = contratoFinanceiroRepository.save(contrato);
        return contratoFinanceiroMapper.toResponseDTO(atualizado);
    }

    // Mexe só nas parcelas em aberto: as pagas continuam sendo as mesmas instâncias na coleção, porque
    // um clear() com orphanRemoval=true as apagaria do banco antes de reinseri-las.
    private void aplicarParcelas(ContratoFinanceiroModel contrato, List<ParcelaContratoRequestDTO> recebidas) {
        List<ParcelaContratoModel> pagas = contrato.getParcelas().stream()
                .filter(p -> p.getDataPagamento() != null)
                .toList();

        for (ParcelaContratoModel paga : pagas) {
            if (recebidas.stream().noneMatch(r -> parcelaInalterada(r, paga))) {
                throw new RegraDeNegocioException(
                        "A parcela " + paga.getNumero() + " já foi paga e não pode ser alterada nem removida.");
            }
        }

        contrato.getParcelas().removeIf(p -> p.getDataPagamento() == null);

        recebidas.stream()
                .filter(r -> pagas.stream().noneMatch(p -> p.getNumero().equals(r.numero())))
                .forEach(r -> contrato.getParcelas().add(ParcelaContratoModel.builder()
                        .contrato(contrato)
                        .numero(r.numero())
                        .dataVencimento(r.dataVencimento())
                        .valor(r.valor())
                        .valorJuros(r.valorJuros())
                        .build()));

        contrato.getParcelas().sort(Comparator.comparing(ParcelaContratoModel::getNumero));
    }

    // valorJuros entra na comparação porque é justamente o que já foi somado a jurosPagos/custoTotal.
    private boolean parcelaInalterada(ParcelaContratoRequestDTO recebida, ParcelaContratoModel paga) {
        return recebida.numero().equals(paga.getNumero())
                && recebida.dataVencimento().equals(paga.getDataVencimento())
                && recebida.valor().compareTo(paga.getValor()) == 0
                && mesmoValor(recebida.valorJuros(), paga.getValorJuros());
    }

    private boolean mesmoValor(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.compareTo(b) == 0;
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

    @Transactional
    public ContratoDocumentoResponseDTO adicionarDocumento(Long contratoId, MultipartFile arquivo,
                                                           TipoDocumentoContrato tipoDocumento, String descricao) {
        ContratoFinanceiroModel contrato = buscarContrato(contratoId);

        String subpasta = "contratos/" + contratoId;
        String nomeArmazenado = storageService.salvar(arquivo, subpasta);
        String url = ArquivoUrls.montar(subpasta, nomeArmazenado);

        ContratoDocumentoModel documento = ContratoDocumentoModel.builder()
                .contrato(contrato)
                .tipoDocumento(tipoDocumento)
                .url(url)
                .nomeArquivo(arquivo.getOriginalFilename())
                .descricao(descricao)
                .build();

        return toDocumentoResponseDTO(contratoDocumentoRepository.save(documento));
    }

    @Transactional(readOnly = true)
    public List<ContratoDocumentoResponseDTO> listarDocumentos(Long contratoId) {
        buscarContrato(contratoId);
        return contratoDocumentoRepository.findByContratoId(contratoId).stream()
                .map(this::toDocumentoResponseDTO)
                .toList();
    }

    @Transactional
    public void deletarDocumento(Long contratoId, Long documentoId) {
        ContratoDocumentoModel documento = contratoDocumentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado com id: " + documentoId));

        if (!documento.getContrato().getId().equals(contratoId)) {
            throw new RegraDeNegocioException("O documento não pertence ao contrato informado.");
        }

        contratoDocumentoRepository.delete(documento);
        storageService.deletar(ArquivoUrls.nomeArquivoDe(documento.getUrl()), ArquivoUrls.subpastaDe(documento.getUrl()));
    }

    private ContratoDocumentoResponseDTO toDocumentoResponseDTO(ContratoDocumentoModel documento) {
        return new ContratoDocumentoResponseDTO(
                documento.getId(),
                documento.getContrato().getId(),
                documento.getTipoDocumento(),
                documento.getUrl(),
                documento.getNomeArquivo(),
                documento.getDescricao(),
                documento.getDataUpload()
        );
    }

    private ContratoFinanceiroModel buscarContrato(Long id) {
        return contratoFinanceiroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato financeiro não encontrado com id: " + id));
    }
}
