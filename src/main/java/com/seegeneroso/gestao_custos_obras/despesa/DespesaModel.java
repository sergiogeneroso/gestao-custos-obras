package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.etapaProjeto.EtapaProjetoModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "imovel_id")
    private ImovelModel imovel;

    @ManyToOne(optional = false)
    @JoinColumn(name = "etapa_projeto_id")
    private EtapaProjetoModel etapaProjeto;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    private String descricao;

    @Column(name = "comprovante_url", length = 500)
    private String comprovanteUrl;

    @Builder.Default
    @OneToMany(mappedBy = "despesa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DespesaPagamentoModel> pagamentos = new ArrayList<>();
}