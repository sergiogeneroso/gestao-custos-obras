package com.seegeneroso.gestao_custos_obras.shared.enums;

/**
 * Recorte interno da fase CONSTRUCAO, para responder quanto custou cada etapa da obra.
 *
 * Não substitui nem concorre com CategoriaDespesa (ADR-026): categoria é a natureza do gasto
 * (material, mão de obra), etapa é o trecho da obra em que ele foi consumido. Só faz sentido em
 * despesa cuja faseImovel é CONSTRUCAO — DespesaService recusa a combinação contrária.
 *
 * Lista fixa de propósito: é vocabulário de obra, não catálogo que o usuário administra.
 */
public enum EtapaConstrucao {
    SERVICOS_PRELIMINARES,
    FUNDACAO,
    ESTRUTURA,
    ALVENARIA,
    COBERTURA,
    INSTALACOES,
    ESQUADRIAS,
    REVESTIMENTO,
    PINTURA,
    ACABAMENTO,
    AREA_EXTERNA,
    OUTRO
}
