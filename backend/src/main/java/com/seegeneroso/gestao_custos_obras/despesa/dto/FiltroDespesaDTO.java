package com.seegeneroso.gestao_custos_obras.despesa.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.EtapaConstrucao;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;

import java.time.LocalDate;

/**
 * Filtros da tela de listagem de despesas, todos opcionais e combinados com "e". Nulo = não filtra.
 * escopo: TODAS, IMOVEL (qualquer imóvel) ou GERAL (sem imóvel); imovelId estreita para um imóvel só.
 * O período compara a data de pagamento. conferida: true = só conferidas, false = só pendentes
 * (ADR-048).
 */
public record FiltroDespesaDTO(
        String busca,
        String escopo,
        Long imovelId,
        Long categoriaDespesaId,
        Long pagadorId,
        Long beneficiarioId,
        FaseImovel faseImovel,
        EtapaConstrucao etapaConstrucao,
        LocalDate dataInicio,
        LocalDate dataFim,
        Boolean semComprovante,
        Boolean conferida
) {
}
