package com.seegeneroso.gestao_custos_obras.imovel;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

// Propriedades da fase LOTE (ADR-031): o que identifica o imóvel juridicamente e perante a
// prefeitura. Nenhuma é obrigatória — matrícula e inscrição costumam chegar depois da compra.
@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DadosLote {

    @Column(name = "lote_matricula", length = 50)
    private String matricula;

    @Column(name = "lote_cartorio", length = 150)
    private String cartorio;

    @Column(name = "lote_data_registro")
    private LocalDate dataRegistro;

    @Column(name = "lote_inscricao_municipal", length = 50)
    private String inscricaoMunicipal;

    // Coluna area_lote preservada da ADR-030 para não migrar o dado de novo.
    @Column(name = "area_lote", precision = 10, scale = 2)
    private BigDecimal area;
}
