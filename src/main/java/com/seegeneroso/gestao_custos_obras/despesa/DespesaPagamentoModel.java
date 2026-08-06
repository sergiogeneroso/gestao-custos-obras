package com.seegeneroso.gestao_custos_obras.despesa;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import com.seegeneroso.gestao_custos_obras.aportante.AportanteModel;

@Entity
@Table(name = "despesa_pagamento")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DespesaPagamentoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "despesa_id")
    private DespesaModel despesa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "envolvido_id")
    private AportanteModel envolvido;

    @Column(name = "valor_pago", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorPago;
}