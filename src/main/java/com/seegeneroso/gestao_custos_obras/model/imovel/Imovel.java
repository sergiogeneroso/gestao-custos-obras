package com.seegeneroso.gestao_custos_obras.model.imovel;

import com.seegeneroso.shared.enums.StatusImovel;
import com.seegeneroso.shared.enums.TipoImovel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "imovel")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Imovel {

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