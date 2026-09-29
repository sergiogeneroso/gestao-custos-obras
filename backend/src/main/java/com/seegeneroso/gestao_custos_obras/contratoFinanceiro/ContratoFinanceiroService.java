package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.AlocacaoLoteRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoDocumentoResponseDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoEstornoRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroResponseDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoQuitacaoRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ParcelaContratoRequestDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ParcelaPagamentoRequestDTO;
import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoDocumentoContrato;
import com.seegeneroso.gestao_custos_obras.shared.Buscas;
import com.seegeneroso.gestao_custos_obras.shared.PaginaDTO;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.AuditoriaService;
import com.seegeneroso.gestao_custos_obras.shared.auditoria.OperacaoAuditoria;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import com.seegeneroso.gestao_custos_obras.shared.storage.ArquivoUrls;
import com.seegeneroso.gestao_custos_obras.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final DespesaRepository despesaRepository;
    private final StorageService storageService;
    private final ContratoFinanceiroMapper contratoFinanceiroMapper;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final AuditoriaService auditoriaService;

    @Transactional
    public ContratoFinanceiroResponseDTO criar(ContratoFinanceiroRequestDTO dto) {
        List<ImovelModel> imoveis = buscarImoveisAtivos(dto.imoveis());
        PessoaModel contraparte = pessoaRepository.findByIdAndAtivoTrue(dto.contraparteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contraparte não encontrada com id: " + dto.contraparteId()));

        ContratoFinanceiroModel contrato = ContratoFinanceiroModel.builder()
                .tipo(dto.tipo())
                .contraparte(contraparte)
                .valorContratado(dto.valorContratado())
                .parcelas(new ArrayList<>())
                .imoveis(new ArrayList<>())
                .build();

        for (int i = 0; i < imoveis.size(); i++) {
            AlocacaoLoteRequestDTO alocacaoDto = dto.imoveis().get(i);
            BigDecimal valorAlocado = alocacaoDto.valorAlocado() != null ? alocacaoDto.valorAlocado() : dto.valorContratado();
            contrato.getImoveis().add(ContratoImovelModel.builder()
                    .contrato(contrato)
                    .imovel(imoveis.get(i))
                    .valorAlocado(valorAlocado)
                    .build());
        }
        validarAlocacoesFecham(contrato);

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
        aplicarValorDoLote(salvo, dto);
        ContratoFinanceiroResponseDTO responseDto = contratoFinanceiroMapper.toResponseDTO(salvo);
        auditoriaService.registrar("ContratoFinanceiro", salvo.getId(), OperacaoAuditoria.CRIACAO, null, responseDto);
        return responseDto;
    }

    private List<ImovelModel> buscarImoveisAtivos(List<AlocacaoLoteRequestDTO> alocacoes) {
        return alocacoes.stream()
                .map(a -> imovelRepository.findByIdAndAtivoTrue(a.imovelId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + a.imovelId())))
                .toList();
    }

    // Sem rateio automático (ADR-047): a soma das alocações precisa fechar exatamente com o valor
    // contratado — diferente da soma de parcela-vs-contratado, aqui não há juros que justifiquem
    // folga.
    private void validarAlocacoesFecham(ContratoFinanceiroModel contrato) {
        BigDecimal somaAlocacoes = contrato.getImoveis().stream()
                .map(ContratoImovelModel::getValorAlocado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (somaAlocacoes.compareTo(contrato.getValorContratado()) != 0) {
            throw new RegraDeNegocioException(
                    "A soma do valor alocado dos imóveis (" + somaAlocacoes
                            + ") precisa ser igual ao valor contratado (" + contrato.getValorContratado() + ").");
        }
    }

    // A entrada é fato consumado no momento da compra, não evento futuro: nasce como parcela nº 0 já
    // baixada, reaproveitando total pago e saldo devedor sem caso especial (ADR-037).
    private ParcelaContratoModel montarEntrada(ContratoFinanceiroModel contrato, ContratoFinanceiroRequestDTO dto) {
        LocalDate data = dto.entradaData() != null
                ? dto.entradaData()
                : contrato.getImoveis().get(0).getImovel().getCompra().getData();
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
     * o preço à vista informado (do negócio inteiro, todos os lotes somados), ou o total do
     * cronograma quando não houver — que é o caso normal, porque o parcelamento do lote costuma ser
     * sem juros. Contrato compartilhado (ADR-047): cada lote reconhece a fatia proporcional à sua
     * alocação — para um único lote a fatia é sempre 1, então o valor gravado é idêntico ao de antes
     * da ADR-047.
     *
     * Só na criação, e só se estiver vazio: reescrever o valor ao editar o cronograma mudaria em
     * silêncio o custo de um imóvel já apurado.
     */
    private void aplicarValorDoLote(ContratoFinanceiroModel contrato, ContratoFinanceiroRequestDTO dto) {
        if (contrato.getTipo() != TipoContratoFinanceiro.PARCELAMENTO_COMPRA) {
            return;
        }

        BigDecimal totalCronograma = contrato.getParcelas().stream()
                .map(ParcelaContratoModel::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal precoAVistaTotal = dto.precoAVistaLote() != null ? dto.precoAVistaLote() : totalCronograma;

        for (ContratoImovelModel alocacao : contrato.getImoveis()) {
            ImovelModel imovel = alocacao.getImovel();
            if (imovel.getCompra().getValor() != null) {
                continue;
            }
            BigDecimal fracao = alocacao.getValorAlocado().divide(contrato.getValorContratado(), 10, RoundingMode.HALF_UP);
            imovel.getCompra().setValor(precoAVistaTotal.multiply(fracao).setScale(2, RoundingMode.HALF_UP));
            imovelRepository.save(imovel);
        }
    }

    /**
     * Edição do contrato. Duas coisas não podem ser reescritas, por ADR-025:
     * - contrato QUITADO é histórico fechado (a quitação tem valor próprio, negociado, e as parcelas
     *   originais precisam continuar legíveis);
     * - parcela já paga não pode ter valor, juros, vencimento ou número alterados, nem ser removida,
     *   porque o valorJuros dela já entrou em jurosPagos/custoTotal do relatório.
     * Não validar a soma das parcelas contra valorContratado: juros fazem a soma exceder o principal
     * legitimamente.
     *
     * O vínculo com os imóveis (ADR-047) é fixado na criação e não é editável aqui — {@code
     * dto.imoveis()} é ignorado. Editar valorContratado depois não recalcula nem revalida as
     * alocações já existentes: elas são valores absolutos declarados uma vez, e a fração de cada
     * lote (valorAlocado ÷ valorContratado) simplesmente se ajusta ao novo total — mesmo espírito
     * de "não validar parcela contra valorContratado" logo abaixo, só que aqui não há nem juros
     * para justificar a folga, é só edição corrigindo um número já lançado.
     */
    @Transactional
    public ContratoFinanceiroResponseDTO atualizar(Long id, ContratoFinanceiroRequestDTO dto) {
        ContratoFinanceiroModel contrato = buscarContrato(id);
        ContratoFinanceiroResponseDTO estadoAnterior = contratoFinanceiroMapper.toResponseDTO(contrato);

        if (contrato.getSituacao() == SituacaoContrato.QUITADO) {
            throw new RegraDeNegocioException("Contrato quitado não pode ser editado.");
        }

        PessoaModel contraparte = pessoaRepository.findByIdAndAtivoTrue(dto.contraparteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contraparte não encontrada com id: " + dto.contraparteId()));

        contrato.setTipo(dto.tipo());
        contrato.setContraparte(contraparte);
        contrato.setValorContratado(dto.valorContratado());

        aplicarParcelas(contrato, dto.parcelas() != null ? dto.parcelas() : List.of());

        ContratoFinanceiroModel atualizado = contratoFinanceiroRepository.save(contrato);
        ContratoFinanceiroResponseDTO estadoNovo = contratoFinanceiroMapper.toResponseDTO(atualizado);
        auditoriaService.registrar("ContratoFinanceiro", id, OperacaoAuditoria.EDICAO, estadoAnterior, estadoNovo);
        return estadoNovo;
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

    /**
     * Lista completa (ou a do imóvel). É o que alimenta o combo de contrato do lançamento de
     * despesa e a conferência do cronograma no cadastro do imóvel. A tela de listagem usa
     * {@link #buscar}.
     */
    @Transactional(readOnly = true)
    public List<ContratoFinanceiroResponseDTO> listar(Long imovelId) {
        List<ContratoFinanceiroModel> contratos = imovelId != null
                ? contratoFinanceiroRepository.findByImovelId(imovelId)
                : contratoFinanceiroRepository.findAll().stream()
                        .filter(c -> Boolean.TRUE.equals(c.getExclusao().getAtivo()))
                        .toList();
        return contratos.stream().map(contratoFinanceiroMapper::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ContratoFinanceiroResponseDTO> buscar(String busca, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id"));
        return PaginaDTO.de(contratoFinanceiroRepository.buscar(Buscas.normalizar(busca), pageable),
                contratoFinanceiroMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ContratoFinanceiroResponseDTO buscarPorId(Long id) {
        return contratoFinanceiroMapper.toResponseDTO(buscarContrato(id));
    }

    @Transactional
    public ContratoFinanceiroResponseDTO quitar(Long id, ContratoQuitacaoRequestDTO dto) {
        ContratoFinanceiroModel contrato = buscarContrato(id);
        ContratoFinanceiroResponseDTO estadoAnterior = contratoFinanceiroMapper.toResponseDTO(contrato);

        if (contrato.getSituacao() == SituacaoContrato.QUITADO) {
            throw new RegraDeNegocioException("Contrato já está quitado.");
        }

        contrato.setSituacao(SituacaoContrato.QUITADO);
        contrato.setDataQuitacao(dto.dataQuitacao());
        contrato.setValorQuitacao(dto.valorQuitacao());

        ContratoFinanceiroModel atualizado = contratoFinanceiroRepository.save(contrato);
        ContratoFinanceiroResponseDTO estadoNovo = contratoFinanceiroMapper.toResponseDTO(atualizado);
        auditoriaService.registrar("ContratoFinanceiro", id, OperacaoAuditoria.EDICAO, estadoAnterior, estadoNovo);
        return estadoNovo;
    }

    /**
     * Cascata acionada por {@code ImovelService} ao desfazer uma venda (ADR-043), só para
     * PARCELAMENTO_VENDA com ao menos uma parcela paga — sem parcela paga, o contrato é excluído
     * via {@link #excluir}. Gera evento de auditoria próprio: diferente da cascata de exclusão do
     * imóvel inteiro (silenciosa, ADR-042), desfazer uma venda é uma operação normal de uso que
     * muda o estado de um único contrato, não uma exclusão em massa.
     */
    @Transactional
    public void cancelarPorVendaDesfeita(Long id, String motivo, LocalDate data) {
        ContratoFinanceiroModel contrato = buscarContrato(id);
        ContratoFinanceiroResponseDTO estadoAnterior = contratoFinanceiroMapper.toResponseDTO(contrato);

        contrato.setSituacao(SituacaoContrato.CANCELADO);
        contrato.setDataCancelamento(data);
        contrato.setMotivoCancelamento(motivo);
        contrato.setValorEstornado(BigDecimal.ZERO);

        ContratoFinanceiroModel atualizado = contratoFinanceiroRepository.save(contrato);
        ContratoFinanceiroResponseDTO estadoNovo = contratoFinanceiroMapper.toResponseDTO(atualizado);
        auditoriaService.registrar("ContratoFinanceiro", id, OperacaoAuditoria.EDICAO, estadoAnterior, estadoNovo);
    }

    // Soma das parcelas efetivamente pagas — o que precisa ser devolvido quando a venda cai.
    private BigDecimal totalPago(ContratoFinanceiroModel contrato) {
        return contrato.getParcelas().stream()
                .filter(p -> p.getDataPagamento() != null)
                .map(ParcelaContratoModel::getValorPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Baixa parcial ou total do estorno de um contrato CANCELADO (ADR-043). {@code valorEstornado}
     * só cresce a cada chamada; "quanto falta devolver" é sempre calculado
     * (totalPago − valorEstornado), nunca gravado — mesmo espírito de nunca alterar os valores
     * originais das parcelas. Não existe cronograma de parcelas de estorno, de propósito: seria
     * duplicar a máquina de {@code ParcelaContratoModel} para devoluções avulsas sem parcelamento
     * negociado de verdade.
     */
    @Transactional
    public ContratoFinanceiroResponseDTO registrarEstorno(Long id, ContratoEstornoRequestDTO dto) {
        ContratoFinanceiroModel contrato = buscarContrato(id);
        ContratoFinanceiroResponseDTO estadoAnterior = contratoFinanceiroMapper.toResponseDTO(contrato);

        if (contrato.getSituacao() != SituacaoContrato.CANCELADO) {
            throw new RegraDeNegocioException("Só é possível registrar estorno em contrato cancelado.");
        }

        BigDecimal jaEstornado = contrato.getValorEstornado() != null ? contrato.getValorEstornado() : BigDecimal.ZERO;
        BigDecimal saldoAEstornar = totalPago(contrato).subtract(jaEstornado);
        if (dto.valor().compareTo(saldoAEstornar) > 0) {
            throw new RegraDeNegocioException("Valor do estorno maior que o saldo ainda a devolver.");
        }

        contrato.setValorEstornado(jaEstornado.add(dto.valor()));
        contrato.setDataEstorno(dto.data());

        ContratoFinanceiroModel atualizado = contratoFinanceiroRepository.save(contrato);
        ContratoFinanceiroResponseDTO estadoNovo = contratoFinanceiroMapper.toResponseDTO(atualizado);
        auditoriaService.registrar("ContratoFinanceiro", id, OperacaoAuditoria.EDICAO, estadoAnterior, estadoNovo);
        return estadoNovo;
    }

    /**
     * Dar baixa numa parcela. Recusa (ADR-044) os três casos em que a parcela não pode mais mudar
     * de mãos: contrato já {@code QUITADO} ou {@code CANCELADO} (histórico fechado, mesma trava de
     * {@link #atualizar}/{@link #excluir}) e parcela que já foi paga (não existe "pagar de novo").
     */
    @Transactional
    public ContratoFinanceiroResponseDTO pagarParcela(Long contratoId, Long parcelaId, ParcelaPagamentoRequestDTO dto) {
        ContratoFinanceiroModel contrato = buscarContrato(contratoId);
        ContratoFinanceiroResponseDTO estadoAnterior = contratoFinanceiroMapper.toResponseDTO(contrato);

        if (contrato.getSituacao() == SituacaoContrato.QUITADO) {
            throw new RegraDeNegocioException("Contrato quitado não pode receber pagamento de parcela.");
        }
        if (contrato.getSituacao() == SituacaoContrato.CANCELADO) {
            throw new RegraDeNegocioException("Contrato cancelado não pode receber pagamento de parcela.");
        }

        ParcelaContratoModel parcela = parcelaContratoRepository.findById(parcelaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Parcela não encontrada com id: " + parcelaId));

        if (!parcela.getContrato().getId().equals(contratoId)) {
            throw new RegraDeNegocioException("A parcela não pertence ao contrato informado.");
        }
        if (parcela.getDataPagamento() != null) {
            throw new RegraDeNegocioException("A parcela já foi paga.");
        }

        parcela.setDataPagamento(dto.dataPagamento());
        parcela.setValorPago(dto.valorPago());
        parcelaContratoRepository.save(parcela);

        ContratoFinanceiroResponseDTO estadoNovo = contratoFinanceiroMapper.toResponseDTO(contrato);
        auditoriaService.registrar("ContratoFinanceiro", contratoId, OperacaoAuditoria.EDICAO, estadoAnterior, estadoNovo);
        return estadoNovo;
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
        return contratoFinanceiroRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato financeiro não encontrado com id: " + id));
    }

    // Endpoint avulso de exclusão (correção de cadastro errado, ADR-040). Trava espelhando a de
    // atualizar(): contrato QUITADO ou com parcela já paga já entrou no resultado apurado de
    // algum relatório — excluir mudaria isso silenciosamente.
    // O registro de auditoria fica aqui, não em cascatearExclusao: a cascata disparada pela
    // exclusão do imóvel inteiro (ImovelExclusaoService) não gera evento próprio de contrato,
    // só o do imóvel (ver .agents/rules/auditoria.md).
    @Transactional
    public void excluir(Long id, String motivo) {
        ContratoFinanceiroModel contrato = buscarContrato(id);
        ContratoFinanceiroResponseDTO estadoAnterior = contratoFinanceiroMapper.toResponseDTO(contrato);

        if (contrato.getSituacao() == SituacaoContrato.QUITADO) {
            throw new RegraDeNegocioException("Contrato quitado não pode ser excluído.");
        }
        boolean temParcelaPaga = contrato.getParcelas().stream().anyMatch(p -> p.getDataPagamento() != null);
        if (temParcelaPaga) {
            throw new RegraDeNegocioException("Contrato com parcela já paga não pode ser excluído.");
        }

        cascatearExclusao(contrato, motivo, usuarioAutenticadoService.usuarioAtual());
        auditoriaService.registrar("ContratoFinanceiro", id, OperacaoAuditoria.EXCLUSAO, estadoAnterior,
                contratoFinanceiroMapper.toResponseDTO(contrato));
    }

    /**
     * Cascata "pura", sem a trava de QUITADO/parcela paga acima — usada pelo endpoint avulso
     * (depois de validar) e por {@link #desvincularImovel}, quando o lote excluído é o último do
     * contrato. A exclusão do imóvel inteiro é sempre permitida (ADR-040), mesmo com contrato
     * quitado ou com parcela já paga.
     */
    @Transactional
    public void cascatearExclusao(ContratoFinanceiroModel contrato, String motivo, UsuarioModel usuario) {
        contrato.getParcelas().forEach(p -> p.getExclusao().excluir(motivo, usuario));

        for (ContratoDocumentoModel documento : contratoDocumentoRepository.findByContratoId(contrato.getId())) {
            contratoDocumentoRepository.delete(documento);
            storageService.deletar(ArquivoUrls.nomeArquivoDe(documento.getUrl()), ArquivoUrls.subpastaDe(documento.getUrl()));
        }

        // Custo acessório do financiamento é gasto real: desvincula, nunca exclui a despesa.
        List<DespesaModel> despesasVinculadas = despesaRepository.findByContratoFinanceiroId(contrato.getId());
        despesasVinculadas.forEach(d -> d.setContratoFinanceiro(null));
        despesaRepository.saveAll(despesasVinculadas);

        List<ImovelModel> imoveisVinculados = contrato.getImoveis().stream().map(ContratoImovelModel::getImovel).toList();

        contrato.getExclusao().excluir(motivo, usuario);
        contratoFinanceiroRepository.save(contrato);

        // PARCELAMENTO_COMPRA grava imovel.compra.valor na criação (aplicarValorDoLote). Para cada
        // lote deste contrato (ADR-047), se não sobrar nenhum outro PARCELAMENTO_COMPRA ativo nele,
        // limpar — evita conservar um preço que veio do contrato que acabou de ser excluído.
        if (contrato.getTipo() == TipoContratoFinanceiro.PARCELAMENTO_COMPRA) {
            for (ImovelModel imovel : imoveisVinculados) {
                limparValorDoLoteSeUltimoParcelamento(imovel, contrato.getId());
            }
        }
    }

    /**
     * Desfaz o vínculo de UM lote com o contrato (ADR-047), acionado por
     * {@code ImovelExclusaoService} ao excluir esse imóvel. Se for o único lote do contrato,
     * cascateia a exclusão do contrato inteiro ({@link #cascatearExclusao}, sempre permitida,
     * mesmo QUITADO ou com parcela paga); senão, remove só a linha de alocação dele — contrato e
     * parcelas continuam intactos para os lotes que sobraram, porque o pagamento é indivisível
     * entre eles.
     */
    @Transactional
    public void desvincularImovel(ContratoFinanceiroModel contrato, Long imovelId, String motivo, UsuarioModel usuario) {
        if (contrato.getImoveis().size() <= 1) {
            cascatearExclusao(contrato, motivo, usuario);
            return;
        }

        ImovelModel imovel = contrato.getImoveis().stream()
                .filter(ci -> ci.getImovel().getId().equals(imovelId))
                .map(ContratoImovelModel::getImovel)
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não vinculado a este contrato: " + imovelId));

        contrato.getImoveis().removeIf(ci -> ci.getImovel().getId().equals(imovelId));
        contratoFinanceiroRepository.save(contrato);

        if (contrato.getTipo() == TipoContratoFinanceiro.PARCELAMENTO_COMPRA) {
            limparValorDoLoteSeUltimoParcelamento(imovel, contrato.getId());
        }
    }

    // contratoExcluidoId é sempre filtrado fora do resultado de findByImovelId, mesmo já não
    // devendo mais aparecer ali (contrato inativo ou vínculo já removido) — defesa contra o
    // mock de teste devolver uma lista estática que não reflete a mutação que acabou de acontecer.
    private void limparValorDoLoteSeUltimoParcelamento(ImovelModel imovel, Long contratoExcluidoId) {
        boolean restaOutroParcelamentoCompra = contratoFinanceiroRepository.findByImovelId(imovel.getId())
                .stream()
                .anyMatch(c -> !c.getId().equals(contratoExcluidoId) && c.getTipo() == TipoContratoFinanceiro.PARCELAMENTO_COMPRA);
        if (!restaOutroParcelamentoCompra) {
            imovel.getCompra().setValor(null);
            imovelRepository.save(imovel);
        }
    }
}
