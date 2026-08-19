package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// Propriedades da fase CONSTRUCAO (ADR-031). Nenhuma é obrigatória na transição: alvará e CNO
// saem depois do início da obra, e a metragem final às vezes só fecha no habite-se.
@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DadosConstrucao {

    // Colunas herdadas de quando estes campos eram soltos no imóvel — preservadas para não migrar.
    @Column(name = "area_construida", precision = 10, scale = 2)
    private BigDecimal area;

    @Column(name = "data_inicio_construcao")
    private LocalDate dataInicio;

    @Column(name = "previsao_conclusao")
    private LocalDate previsaoConclusao;

    @Column(name = "custo_estimado_obra", precision = 14, scale = 2)
    private BigDecimal custoEstimado;

    @Column(name = "construcao_alvara_numero", length = 50)
    private String alvaraNumero;

    @Column(name = "construcao_alvara_emissao")
    private LocalDate alvaraEmissao;

    @Column(name = "construcao_alvara_validade")
    private LocalDate alvaraValidade;

    @Column(name = "construcao_art_numero", length = 50)
    private String artNumero;

    @ManyToOne
    @JoinColumn(name = "construcao_responsavel_tecnico_id")
    private PessoaModel responsavelTecnico;

    // Matrícula da obra na Receita (ex-CEI): sem ela não se emite a CND para averbar a construção.
    @Column(name = "construcao_cno", length = 50)
    private String cno;
}
