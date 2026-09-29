package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.AlocacaoLoteResponseDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ContratoFinanceiroResponseDTO;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.dto.ParcelaContratoResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ContratoFinanceiroMapper {

    public AlocacaoLoteResponseDTO toAlocacaoResponseDTO(ContratoImovelModel alocacao) {
        return new AlocacaoLoteResponseDTO(
                alocacao.getImovel().getId(),
                alocacao.getImovel().getIdentificador(),
                alocacao.getValorAlocado()
        );
    }

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
                contrato.getImoveis().stream().map(this::toAlocacaoResponseDTO).toList(),
                contrato.getTipo(),
                contrato.getContraparte() != null ? contrato.getContraparte().getId() : null,
                contrato.getContraparte() != null ? contrato.getContraparte().getNome() : null,
                contrato.getValorContratado(),
                contrato.getSituacao(),
                contrato.getDataQuitacao(),
                contrato.getValorQuitacao(),
                contrato.getDataCancelamento(),
                contrato.getMotivoCancelamento(),
                contrato.getValorEstornado(),
                contrato.getDataEstorno(),
                contrato.getParcelas().stream().map(this::toParcelaResponseDTO).toList()
        );
    }
}
