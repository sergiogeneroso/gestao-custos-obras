package com.seegeneroso.gestao_custos_obras.imovel;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// Propriedades da fase CASA (ADR-031): os documentos que fazem a construção existir juridicamente
// e as características que se usam para vender.
@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DadosCasa {

    // Coluna herdada de quando o campo era solto no imóvel — preservada para não migrar.
    @Column(name = "data_conclusao_obra")
    private LocalDate dataConclusaoObra;

    @Column(name = "casa_habite_se_numero", length = 50)
    private String habiteSeNumero;

    @Column(name = "casa_habite_se_data")
    private LocalDate habiteSeData;

    // Averbação da construção na matrícula: sem ela a venda financiada trava.
    @Column(name = "casa_data_averbacao")
    private LocalDate dataAverbacao;

    @Column(name = "casa_quartos")
    private Integer quartos;

    @Column(name = "casa_suites")
    private Integer suites;

    @Column(name = "casa_banheiros")
    private Integer banheiros;

    @Column(name = "casa_vagas_garagem")
    private Integer vagasGaragem;
}
