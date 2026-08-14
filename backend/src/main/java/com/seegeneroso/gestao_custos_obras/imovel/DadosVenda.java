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
public class DadosVenda {

    @Column(name = "venda_valor", precision = 14, scale = 2)
    private BigDecimal valor;

    @Column(name = "venda_data")
    private LocalDate data;

    @ManyToOne
    @JoinColumn(name = "venda_comprador_id")
    private PessoaModel comprador;

    @Column(name = "venda_valor_pretendido", precision = 14, scale = 2)
    private BigDecimal valorPretendido;
}
