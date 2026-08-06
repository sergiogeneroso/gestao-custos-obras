package com.seegeneroso.gestao_custos_obras.imovel;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import com.seegeneroso.gestao_custos_obras.shared.enums.StatusImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoImovel;

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
    private TipoImovel tipo;

    private String endereco;

    @Column(precision = 10, scale = 2)
    private BigDecimal area;

    @Column(name = "valor_aquisicao_inicial", precision = 14, scale = 2)
    private BigDecimal valorAquisicaoInicial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusImovel status = StatusImovel.PLANEJAMENTO;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}