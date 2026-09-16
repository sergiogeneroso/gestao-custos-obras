package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.shared.exclusao.ExclusaoLogica;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "parcela_contrato")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ParcelaContratoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contrato_id")
    private ContratoFinanceiroModel contrato;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "valor_juros", precision = 14, scale = 2)
    private BigDecimal valorJuros;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "valor_pago", precision = 14, scale = 2)
    private BigDecimal valorPago;

    // columnDefinition com DEFAULT TRUE: ver o mesmo comentário em ContratoFinanceiroModel.
    @Embedded
    @AttributeOverride(name = "ativo", column = @Column(name = "ativo", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE"))
    @Builder.Default
    private ExclusaoLogica exclusao = new ExclusaoLogica();

    // Getter manual: mesmo motivo do de ImovelModel — @ManyToOne dentro do embeddable
    // (excluidoPor) faz o Hibernate devolver null em vez do objeto vazio.
    public ExclusaoLogica getExclusao() {
        if (exclusao == null) {
            exclusao = new ExclusaoLogica();
        }
        return exclusao;
    }
}
