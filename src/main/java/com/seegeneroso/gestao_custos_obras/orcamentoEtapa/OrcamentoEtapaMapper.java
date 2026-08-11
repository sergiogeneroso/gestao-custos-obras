package com.seegeneroso.gestao_custos_obras.orcamentoEtapa;

import com.seegeneroso.gestao_custos_obras.orcamentoEtapa.dto.OrcamentoEtapaResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class OrcamentoEtapaMapper {

    public OrcamentoEtapaResponseDTO toResponseDTO(OrcamentoEtapaModel model, BigDecimal totalGasto) {
        BigDecimal gasto = totalGasto != null ? totalGasto : BigDecimal.ZERO;
        BigDecimal valorOrcado = model.getValorOrcado();
        BigDecimal diferenca = valorOrcado.subtract(gasto);

        String status;
        if (valorOrcado.compareTo(BigDecimal.ZERO) == 0) {
            status = "INDEFINIDO";
        } else {
            BigDecimal percentualGasto = gasto.multiply(new BigDecimal("100"))
                    .divide(valorOrcado, 2, RoundingMode.HALF_UP);

            if (percentualGasto.compareTo(new BigDecimal("80")) <= 0) {
                status = "NO_PRAZO";
            } else if (percentualGasto.compareTo(new BigDecimal("100")) <= 0) {
                status = "ATENCAO";
            } else {
                status = "ESTOURADO";
            }
        }

        return new OrcamentoEtapaResponseDTO(
                model.getId(),
                model.getImovel() != null ? model.getImovel().getId() : null,
                model.getImovel() != null ? model.getImovel().getIdentificador() : null,
                model.getEtapaProjeto() != null ? model.getEtapaProjeto().getId() : null,
                model.getEtapaProjeto() != null ? model.getEtapaProjeto().getNome() : null,
                model.getValorOrcado(),
                model.getDataInicioPrevista(),
                model.getDataFimPrevista(),
                gasto,
                diferenca,
                status
        );
    }
}
