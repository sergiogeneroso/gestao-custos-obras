package com.seegeneroso.gestao_custos_obras.despesa.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.EtapaConstrucao;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DespesaResponseDTO(
        Long id,
        Long imovelId,
        String imovelIdentificador,
        Long categoriaDespesaId,
        String categoriaDespesaNome,
        Long pagadorId,
        String pagadorNome,
        Long beneficiarioId,
        String beneficiarioNome,
        Long contratoFinanceiroId,
        FaseImovel faseImovel,
        EtapaConstrucao etapaConstrucao,
        BigDecimal valor,
        LocalDate dataPagamento,
        String descricao,
        String observacao,
        Boolean ativo,
        /**
         * Quantos anexos a despesa tem, de qualquer tipo. Só a busca paginada preenche este campo —
         * nos demais caminhos vem nulo ("não calculado", nunca "sem anexo").
         */
        Integer quantidadeAnexos,

        /**
         * Se a despesa tem ao menos um anexo do tipo COMPROVANTE (a prova de pagamento — RECIBO,
         * NOTA_FISCAL etc. não contam). Só a busca paginada preenche este campo; nulo é "não
         * calculado", nunca "sem comprovante" — a tela usa isso para destacar (sem bloquear) o
         * lançamento que ainda não tem prova de pagamento anexada.
         */
        Boolean temComprovante
) {}
