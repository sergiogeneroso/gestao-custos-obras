package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
import com.seegeneroso.gestao_custos_obras.contratoFinanceiro.ContratoFinanceiroModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.shared.enums.EtapaConstrucao;
import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "despesa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespesaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "imovel_id")
    private ImovelModel imovel;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_despesa_id")
    private CategoriaDespesaModel categoriaDespesa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pagador_id")
    private PessoaModel pagador;

    @ManyToOne
    @JoinColumn(name = "beneficiario_id")
    private PessoaModel beneficiario;

    @ManyToOne
    @JoinColumn(name = "contrato_financeiro_id")
    private ContratoFinanceiroModel contratoFinanceiro;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase_imovel", length = 20)
    private FaseImovel faseImovel;

    // Recorte da obra: só é preenchido quando faseImovel == CONSTRUCAO (DespesaService valida).
    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_construcao", length = 30)
    private EtapaConstrucao etapaConstrucao;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    private String descricao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
