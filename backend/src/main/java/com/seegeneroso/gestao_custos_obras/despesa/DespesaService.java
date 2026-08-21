package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaAnexoResponseDTO;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaRequestDTO;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaResponseDTO;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelRepository;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaRepository;
import com.seegeneroso.gestao_custos_obras.shared.enums.EtapaConstrucao;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoAnexoDespesa;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.exception.RegraDeNegocioException;
import com.seegeneroso.gestao_custos_obras.shared.storage.ArquivoUrls;
import com.seegeneroso.gestao_custos_obras.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final ImovelRepository imovelRepository;
    private final CategoriaDespesaRepository categoriaDespesaRepository;
    private final PessoaRepository pessoaRepository;
    private final ContratoFinanceiroRepository contratoFinanceiroRepository;
    private final DespesaAnexoRepository despesaAnexoRepository;
    private final StorageService storageService;
    private final DespesaMapper despesaMapper;

    @Transactional
    public DespesaResponseDTO criar(DespesaRequestDTO dto) {
        ImovelModel imovel = buscarImovelOpcional(dto.imovelId());
        CategoriaDespesaModel categoria = buscarCategoria(dto.categoriaDespesaId());
        PessoaModel pagador = buscarPessoaAtiva(dto.pagadorId(), "Pagador");
        PessoaModel beneficiario = dto.beneficiarioId() != null ? buscarPessoaAtiva(dto.beneficiarioId(), "Beneficiário") : null;
        ContratoFinanceiroModel contrato = buscarContratoOpcional(dto.contratoFinanceiroId());
        FaseImovel faseImovel = resolverFaseImovel(dto.faseImovel(), imovel);
        validarEtapaConstrucao(dto.etapaConstrucao(), faseImovel);

        DespesaModel despesa = DespesaModel.builder()
                .imovel(imovel)
                .categoriaDespesa(categoria)
                .pagador(pagador)
                .beneficiario(beneficiario)
                .contratoFinanceiro(contrato)
                .faseImovel(faseImovel)
                .etapaConstrucao(dto.etapaConstrucao())
                .valor(dto.valor())
                .dataPagamento(dto.dataPagamento())
                .descricao(dto.descricao())
                .build();

        DespesaModel despesaSalva = despesaRepository.save(despesa);
        return despesaMapper.toResponseDTO(despesaSalva);
    }

    @Transactional
    public DespesaResponseDTO atualizar(Long id, DespesaRequestDTO dto) {
        DespesaModel despesa = despesaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada com id: " + id));

        ImovelModel imovel = buscarImovelOpcional(dto.imovelId());
        CategoriaDespesaModel categoria = buscarCategoria(dto.categoriaDespesaId());
        PessoaModel pagador = buscarPessoaAtiva(dto.pagadorId(), "Pagador");
        PessoaModel beneficiario = dto.beneficiarioId() != null ? buscarPessoaAtiva(dto.beneficiarioId(), "Beneficiário") : null;
        ContratoFinanceiroModel contrato = buscarContratoOpcional(dto.contratoFinanceiroId());
        FaseImovel faseImovel = resolverFaseImovel(dto.faseImovel(), imovel);
        validarEtapaConstrucao(dto.etapaConstrucao(), faseImovel);

        despesa.setImovel(imovel);
        despesa.setCategoriaDespesa(categoria);
        despesa.setPagador(pagador);
        despesa.setBeneficiario(beneficiario);
        despesa.setContratoFinanceiro(contrato);
        despesa.setFaseImovel(faseImovel);
        despesa.setEtapaConstrucao(dto.etapaConstrucao());
        despesa.setValor(dto.valor());
        despesa.setDataPagamento(dto.dataPagamento());
        despesa.setDescricao(dto.descricao());

        DespesaModel despesaAtualizada = despesaRepository.save(despesa);
        return despesaMapper.toResponseDTO(despesaAtualizada);
    }

    @Transactional(readOnly = true)
    public List<DespesaResponseDTO> listar(Long imovelId, Long categoriaDespesaId, Boolean semImovel) {
        List<DespesaModel> despesas;

        if (Boolean.TRUE.equals(semImovel)) {
            despesas = despesaRepository.findByImovelIsNullAndAtivoTrue();
        } else if (imovelId != null && categoriaDespesaId != null) {
            despesas = despesaRepository.findByImovelIdAndCategoriaDespesaIdAndAtivoTrue(imovelId, categoriaDespesaId);
        } else if (imovelId != null) {
            despesas = despesaRepository.findByImovelIdAndAtivoTrue(imovelId);
        } else if (categoriaDespesaId != null) {
            despesas = despesaRepository.findByCategoriaDespesaIdAndAtivoTrue(categoriaDespesaId);
        } else {
            despesas = despesaRepository.findByAtivoTrue();
        }

        return despesas.stream()
                .map(despesaMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DespesaResponseDTO> listarPorImovel(Long imovelId) {
        if (!imovelRepository.existsById(imovelId)) {
            throw new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId);
        }
        return listar(imovelId, null, false);
    }

    @Transactional(readOnly = true)
    public DespesaResponseDTO buscarPorId(Long id) {
        DespesaModel despesa = despesaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada com id: " + id));
        return despesaMapper.toResponseDTO(despesa);
    }

    @Transactional
    public void inativar(Long id) {
        DespesaModel despesa = despesaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada com id: " + id));
        despesa.setAtivo(false);
        despesaRepository.save(despesa);
    }

    private ImovelModel buscarImovelOpcional(Long imovelId) {
        if (imovelId == null) {
            return null;
        }
        return imovelRepository.findByIdAndAtivoTrue(imovelId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId));
    }

    private CategoriaDespesaModel buscarCategoria(Long categoriaDespesaId) {
        return categoriaDespesaRepository.findById(categoriaDespesaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria de despesa não encontrada com id: " + categoriaDespesaId));
    }

    private PessoaModel buscarPessoaAtiva(Long pessoaId, String papel) {
        return pessoaRepository.findByIdAndAtivoTrue(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(papel + " não encontrado com id: " + pessoaId));
    }

    private ContratoFinanceiroModel buscarContratoOpcional(Long contratoFinanceiroId) {
        if (contratoFinanceiroId == null) {
            return null;
        }
        return contratoFinanceiroRepository.findById(contratoFinanceiroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato financeiro não encontrado com id: " + contratoFinanceiroId));
    }

    // A etapa é um recorte da obra: fora da fase CONSTRUCAO ela produziria um número sem sentido
    // no quadro de custo por etapa do relatório.
    private void validarEtapaConstrucao(EtapaConstrucao etapa, FaseImovel fase) {
        if (etapa != null && fase != FaseImovel.CONSTRUCAO) {
            throw new RegraDeNegocioException("Etapa da construção só pode ser informada em despesa da fase Construção.");
        }
    }

    private FaseImovel resolverFaseImovel(FaseImovel faseInformada, ImovelModel imovel) {
        if (faseInformada != null) {
            return faseInformada;
        }
        return imovel != null ? imovel.getFase() : null;
    }

    @Transactional
    public DespesaAnexoResponseDTO adicionarAnexo(Long despesaId, MultipartFile arquivo, TipoAnexoDespesa tipoAnexo) {
        DespesaModel despesa = despesaRepository.findByIdAndAtivoTrue(despesaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa não encontrada com id: " + despesaId));

        String subpasta = "despesas/" + despesaId;
        String nomeArquivo = storageService.salvar(arquivo, subpasta);
        String fileUri = ArquivoUrls.montar(subpasta, nomeArquivo);

        DespesaAnexoModel anexo = DespesaAnexoModel.builder()
                .despesa(despesa)
                .tipoAnexo(tipoAnexo)
                .url(fileUri)
                .build();

        DespesaAnexoModel anexoSalvo = despesaAnexoRepository.save(anexo);
        return toAnexoResponseDTO(anexoSalvo);
    }

    @Transactional(readOnly = true)
    public List<DespesaAnexoResponseDTO> listarAnexos(Long despesaId, TipoAnexoDespesa tipoAnexo) {
        if (!despesaRepository.existsById(despesaId)) {
            throw new RecursoNaoEncontradoException("Despesa não encontrada com id: " + despesaId);
        }
        List<DespesaAnexoModel> anexos = tipoAnexo != null
                ? despesaAnexoRepository.findByDespesaIdAndTipoAnexo(despesaId, tipoAnexo)
                : despesaAnexoRepository.findByDespesaId(despesaId);
        return anexos.stream().map(this::toAnexoResponseDTO).toList();
    }

    @Transactional
    public void deletarAnexo(Long despesaId, Long anexoId) {
        DespesaAnexoModel anexo = despesaAnexoRepository.findById(anexoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Anexo não encontrado com id: " + anexoId));

        if (!anexo.getDespesa().getId().equals(despesaId)) {
            throw new RegraDeNegocioException("O anexo não pertence à despesa informada.");
        }

        despesaAnexoRepository.delete(anexo);
        storageService.deletar(ArquivoUrls.nomeArquivoDe(anexo.getUrl()), ArquivoUrls.subpastaDe(anexo.getUrl()));
    }

    private DespesaAnexoResponseDTO toAnexoResponseDTO(DespesaAnexoModel anexo) {
        return new DespesaAnexoResponseDTO(
                anexo.getId(),
                anexo.getDespesa().getId(),
                anexo.getTipoAnexo(),
                anexo.getUrl(),
                anexo.getDataUpload()
        );
    }
}
