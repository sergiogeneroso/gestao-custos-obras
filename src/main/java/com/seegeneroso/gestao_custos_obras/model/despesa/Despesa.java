package com.seegeneroso.gestao_custos_obras.model.despesa;

import com.seegeneroso.gestao_custos_obras.model.etapa.Etapa;
import com.seegeneroso.gestao_custos_obras.model.imovel.Imovel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "despesa")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "imovel_id")
    private Imovel imovel;

    @ManyToOne(optional = false)
    @JoinColumn(name = "etapa_id")
    private Etapa etapa;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    private String descricao;

    @Column(name = "comprovante_url", length = 500)
    private String comprovanteUrl;
}