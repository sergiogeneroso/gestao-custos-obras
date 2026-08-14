package com.seegeneroso.gestao_custos_obras.imovel;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoImovel;

@Entity
@Table(name = "imovel")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ImovelModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String identificador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FaseImovel fase = FaseImovel.LOTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SituacaoImovel situacao = SituacaoImovel.ADQUIRIDO;

    private String endereco;

    @Column(precision = 10, scale = 2)
    private BigDecimal area;

    @Column(name = "data_inicio_lote", nullable = false)
    private LocalDate dataInicioLote;

    @Column(name = "data_inicio_construcao")
    private LocalDate dataInicioConstrucao;

    @Column(name = "data_conclusao_obra")
    private LocalDate dataConclusaoObra;

    @Column(name = "custo_estimado_obra", precision = 14, scale = 2)
    private BigDecimal custoEstimadoObra;

    @Column(name = "previsao_conclusao")
    private LocalDate previsaoConclusao;

    @Embedded
    @Builder.Default
    private DadosCompra compra = new DadosCompra();

    @Embedded
    @Builder.Default
    private DadosVenda venda = new DadosVenda();

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
