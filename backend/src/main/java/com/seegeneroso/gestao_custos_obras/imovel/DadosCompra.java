package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DadosCompra {

    @Column(name = "compra_valor", precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "compra_data")
    private LocalDate data;

    @ManyToOne
    @JoinColumn(name = "compra_vendedor_id")
    private PessoaModel vendedor;
}
