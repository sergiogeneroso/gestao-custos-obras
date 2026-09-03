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
         * Quantos anexos a despesa tem. Só a busca paginada preenche este campo — nos demais
         * caminhos vem nulo, que significa "não calculado", nunca "sem anexo". Quem consome
         * precisa distinguir os dois: desenhar alerta de comprovante faltando a partir de nulo
         * marcaria como pendente uma despesa que ninguém contou.
         */
        Integer quantidadeAnexos
) {}
