package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroRepository;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroService;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaModel;
import com.seegeneroso.gestao_custos_obras.despesa.DespesaRepository;
import com.seegeneroso.gestao_custos_obras.imovel.dto.ImpactoExclusaoImovelResponseDTO;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.OrcamentoCategoriaModel;
import com.seegeneroso.gestao_custos_obras.orcamentoCategoria.OrcamentoCategoriaRepository;
import com.seegeneroso.gestao_custos_obras.shared.auth.UsuarioAutenticadoService;
import com.seegeneroso.gestao_custos_obras.shared.exception.RecursoNaoEncontradoException;
import com.seegeneroso.gestao_custos_obras.shared.storage.ArquivoUrls;
import com.seegeneroso.gestao_custos_obras.shared.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exclusão em cascata do imóvel (ADR-040) — separado de {@link ImovelService} porque o cadastro
 * já é grande e financeiramente sensível, e a cascata precisa de repositories de 5 domínios
 * diferentes. Sempre permitida, em qualquer fase/situação, mesmo com contrato quitado ou parcela
 * já paga (diferente da exclusão avulsa de contrato, que tem trava — ver
 * ContratoFinanceiroService.excluir).
 */
@Service
@RequiredArgsConstructor
public class ImovelExclusaoService {

    private final ImovelRepository imovelRepository;
    private final DespesaRepository despesaRepository;
    private final ContratoFinanceiroRepository contratoFinanceiroRepository;
    private final ContratoFinanceiroService contratoFinanceiroService;
    private final OrcamentoCategoriaRepository orcamentoCategoriaRepository;
    private final ImovelFotoRepository imovelFotoRepository;
    private final ImovelDocumentoRepository imovelDocumentoRepository;
    private final StorageService storageService;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    @Transactional
    public void excluir(Long imovelId, String motivo) {
        ImovelModel imovel = imovelRepository.findByIdAndAtivoTrue(imovelId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId));
        UsuarioModel usuario = usuarioAutenticadoService.usuarioAtual();

        // Contratos antes de despesas: cascatearExclusao desvincula despesa de custo acessório
        // do contrato, então o contrato precisa estar presente para isso acontecer antes da
        // despesa ser inativada logo abaixo.
        for (ContratoFinanceiroModel contrato : contratoFinanceiroRepository.findByImovelId(imovelId)) {
            contratoFinanceiroService.cascatearExclusao(contrato, motivo, usuario);
        }

        for (DespesaModel despesa : despesaRepository.findByImovelIdAndAtivoTrue(imovelId)) {
            despesa.getExclusao().excluir(motivo, usuario);
        }

        for (OrcamentoCategoriaModel orcamento : orcamentoCategoriaRepository.findByImovelId(imovelId)) {
            orcamento.getExclusao().excluir(motivo, usuario);
        }

        for (ImovelFotoModel foto : imovelFotoRepository.findByImovelId(imovelId)) {
            imovelFotoRepository.delete(foto);
            storageService.deletar(ArquivoUrls.nomeArquivoDe(foto.getUrl()), ArquivoUrls.subpastaDe(foto.getUrl()));
        }

        for (ImovelDocumentoModel documento : imovelDocumentoRepository.findByImovelId(imovelId)) {
            imovelDocumentoRepository.delete(documento);
            storageService.deletar(ArquivoUrls.nomeArquivoDe(documento.getUrl()), ArquivoUrls.subpastaDe(documento.getUrl()));
        }

        imovel.getExclusao().excluir(motivo, usuario);
        imovelRepository.save(imovel);
    }

    @Transactional(readOnly = true)
    public ImpactoExclusaoImovelResponseDTO calcularImpacto(Long imovelId) {
        if (!imovelRepository.existsById(imovelId)) {
            throw new RecursoNaoEncontradoException("Imóvel não encontrado com id: " + imovelId);
        }
        long despesas = despesaRepository.findByImovelIdAndAtivoTrue(imovelId).size();
        long contratos = contratoFinanceiroRepository.findByImovelId(imovelId).size();
        long fotos = imovelFotoRepository.findByImovelId(imovelId).size();
        long documentos = imovelDocumentoRepository.findByImovelId(imovelId).size();
        long orcamentos = orcamentoCategoriaRepository.findByImovelId(imovelId).size();
        return new ImpactoExclusaoImovelResponseDTO(despesas, contratos, fotos, documentos, orcamentos);
    }
}
