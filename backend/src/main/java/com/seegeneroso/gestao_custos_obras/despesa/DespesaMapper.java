package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaPagamentoResponseDTO;
import com.seegeneroso.gestao_custos_obras.despesa.dto.DespesaResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DespesaMapper {

    public DespesaResponseDTO toResponseDTO(DespesaModel despesa) {
        List<DespesaPagamentoResponseDTO> pagamentos = despesa.getPagamentos() == null
                ? Collections.emptyList()
                : despesa.getPagamentos().stream()
                .map(this::toPagamentoResponseDTO)
                .toList();

        return new DespesaResponseDTO(
                despesa.getId(),
                despesa.getImovel() != null ? despesa.getImovel().getId() : null,
                despesa.getImovel() != null ? despesa.getImovel().getIdentificador() : null,
                despesa.getEtapaProjeto() != null ? despesa.getEtapaProjeto().getId() : null,
                despesa.getEtapaProjeto() != null ? despesa.getEtapaProjeto().getNome() : null,
                despesa.getValor(),
                despesa.getDataPagamento(),
                despesa.getDescricao(),
                despesa.getComprovanteUrl(),
                pagamentos
        );
    }

    public DespesaPagamentoResponseDTO toPagamentoResponseDTO(DespesaPagamentoModel pagamento) {
        return new DespesaPagamentoResponseDTO(
                pagamento.getId(),
                pagamento.getAportante() != null ? pagamento.getAportante().getId() : null,
                pagamento.getAportante() != null ? pagamento.getAportante().getNome() : null,
                pagamento.getValorPago()
        );
    }
}
