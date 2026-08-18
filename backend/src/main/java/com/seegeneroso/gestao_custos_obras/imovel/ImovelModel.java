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

    // Duas metragens porque o imóvel muda de natureza ao longo do ciclo (ADR-030): a do lote vem
    // da compra, a construída só existe a partir da obra. Ambas opcionais — a área construída nem
    // sempre é conhecida quando a construção começa.
    @Column(name = "area_lote", precision = 10, scale = 2)
    private BigDecimal areaLote;

    @Column(name = "area_construida", precision = 10, scale = 2)
    private BigDecimal areaConstruida;

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

    // Getters manuais (Lombok não gera os de compra/venda quando já existem): o Hibernate
    // devolve null para um @Embedded com todas as colunas nulas no banco (em vez do objeto
    // com campos null que o @Builder.Default promete para uma instância nova), porque
    // DadosVenda tem uma associação @ManyToOne, fora do que
    // hibernate.create_empty_composites.enabled cobre. Nunca deixar essa entidade devolver
    // null aqui é o que garante que ImovelService e RelatorioService podem tratar
    // imovel.getCompra()/getVenda() como sempre presentes.
    public DadosCompra getCompra() {
        if (compra == null) {
            compra = new DadosCompra();
        }
        return compra;
    }

    public DadosVenda getVenda() {
        if (venda == null) {
            venda = new DadosVenda();
        }
        return venda;
    }
}
