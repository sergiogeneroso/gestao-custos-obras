package com.seegeneroso.gestao_custos_obras.imovel;

import jakarta.persistence.*;
import lombok.*;

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

    // Endereço na raiz, no formato convencional (ADR-031): vale para o imóvel inteiro e não muda
    // de significado quando a fase avança.
    private String endereco;

    @Column(length = 20)
    private String numero;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(length = 9)
    private String cep;

    @Column(name = "observacao_endereco")
    private String observacaoEndereco;

    // Um @Embedded por fase (ADR-031): as propriedades de lote, construção e casa vivem agrupadas
    // aqui, numa tabela só, com os nulos das fases ainda não atingidas sendo esperados.
    @Embedded
    @Builder.Default
    private DadosLote lote = new DadosLote();

    @Embedded
    @Builder.Default
    private DadosConstrucao construcao = new DadosConstrucao();

    @Embedded
    @Builder.Default
    private DadosCasa casa = new DadosCasa();

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

    // Getters manuais (Lombok não gera os que já existem): o Hibernate devolve null para um
    // @Embedded com todas as colunas nulas no banco (em vez do objeto com campos null que o
    // @Builder.Default promete para uma instância nova) quando o embeddable tem uma associação
    // @ManyToOne — caso de DadosVenda e DadosConstrucao —, fora do que
    // hibernate.create_empty_composites.enabled cobre. Os demais seguem o mesmo padrão de
    // propósito: o modo de falha é um NPE no meio de cálculo financeiro, e a defesa custa três
    // linhas. Nunca devolver null aqui é o que garante que ImovelService e RelatorioService podem
    // tratar estes grupos como sempre presentes.
    public DadosLote getLote() {
        if (lote == null) {
            lote = new DadosLote();
        }
        return lote;
    }

    public DadosConstrucao getConstrucao() {
        if (construcao == null) {
            construcao = new DadosConstrucao();
        }
        return construcao;
    }

    public DadosCasa getCasa() {
        if (casa == null) {
            casa = new DadosCasa();
        }
        return casa;
    }

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
