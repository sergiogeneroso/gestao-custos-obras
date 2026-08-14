package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroResponseDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ParcelaContratoResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ContratoFinanceiroMapper {

    public ParcelaContratoResponseDTO toParcelaResponseDTO(ParcelaContratoModel parcela) {
        return new ParcelaContratoResponseDTO(
                parcela.getId(),
                parcela.getNumero(),
                parcela.getDataVencimento(),
                parcela.getValor(),
                parcela.getValorJuros(),
                parcela.getDataPagamento(),
                parcela.getValorPago()
        );
    }

    public ContratoFinanceiroResponseDTO toResponseDTO(ContratoFinanceiroModel contrato) {
        return new ContratoFinanceiroResponseDTO(
                contrato.getId(),
                contrato.getImovel() != null ? contrato.getImovel().getId() : null,
                contrato.getImovel() != null ? contrato.getImovel().getIdentificador() : null,
                contrato.getTipo(),
                contrato.getContraparte() != null ? contrato.getContraparte().getId() : null,
                contrato.getContraparte() != null ? contrato.getContraparte().getNome() : null,
                contrato.getValorContratado(),
                contrato.getSituacao(),
                contrato.getDataQuitacao(),
                contrato.getValorQuitacao(),
                contrato.getParcelas().stream().map(this::toParcelaResponseDTO).toList()
        );
    }
}
