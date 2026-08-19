package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class DespesaMapper {

    public DespesaResponseDTO toResponseDTO(DespesaModel despesa) {
        return new DespesaResponseDTO(
                despesa.getId(),
                despesa.getImovel() != null ? despesa.getImovel().getId() : null,
                despesa.getImovel() != null ? despesa.getImovel().getIdentificador() : null,
                despesa.getCategoriaDespesa() != null ? despesa.getCategoriaDespesa().getId() : null,
                despesa.getCategoriaDespesa() != null ? despesa.getCategoriaDespesa().getNome() : null,
                despesa.getPagador() != null ? despesa.getPagador().getId() : null,
                despesa.getPagador() != null ? despesa.getPagador().getNome() : null,
                despesa.getBeneficiario() != null ? despesa.getBeneficiario().getId() : null,
                despesa.getBeneficiario() != null ? despesa.getBeneficiario().getNome() : null,
                despesa.getContratoFinanceiro() != null ? despesa.getContratoFinanceiro().getId() : null,
                despesa.getFaseImovel(),
                despesa.getEtapaConstrucao(),
                despesa.getValor(),
                despesa.getDataPagamento(),
                despesa.getDescricao(),
                despesa.getAtivo()
        );
    }
}
