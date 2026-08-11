package com.seegeneroso.gestao_custos_obras.despesa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DespesaResponseDTO(
        Long id,
        Long imovelId,
        String imovelIdentificador,
        Long etapaProjetoId,
        String etapaProjetoNome,
        BigDecimal valor,
        LocalDate dataPagamento,
        String descricao,
        String comprovanteUrl,
        List<DespesaPagamentoResponseDTO> pagamentos
) {}
