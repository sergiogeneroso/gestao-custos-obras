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

    // Marco inicial da carteira e da fase LOTE (ADR-032) — não existe dataInicioLote separada.
    @Column(name = "compra_data", nullable = false)
    private LocalDate data;

    // Declarado no cadastro, não derivado da existência de contrato: é o que diz ao relatório se o
    // valor de compra saiu do bolso na data da compra ou está diluído no cronograma (ADR-037).
    // O DEFAULT no DDL é obrigatório — sem ele o ddl-auto=update não adiciona coluna NOT NULL a
    // uma tabela que já tem linhas.
    @Column(name = "compra_parcelada", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean parcelada = false;

    @ManyToOne
    @JoinColumn(name = "compra_vendedor_id")
    private PessoaModel vendedor;
}
