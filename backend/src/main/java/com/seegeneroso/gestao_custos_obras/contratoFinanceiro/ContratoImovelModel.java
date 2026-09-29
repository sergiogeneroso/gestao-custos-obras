package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// Alocação de um lote dentro de um contrato compartilhado (ADR-047): o vínculo entre
// ContratoFinanceiro e Imovel deixou de ser 1:1 porque um único PARCELAMENTO_COMPRA ou
// PARCELAMENTO_VENDA pode cobrir vários lotes ao mesmo tempo (mesma entrada, mesmo cronograma,
// pagamento indivisível). valorAlocado é declarado à mão pelo usuário, nunca calculado — mesmo
// princípio de "sem rateio automático" da despesa compartilhada. Sem ExclusaoLogica própria: o
// vínculo só existe enquanto o lote está no contrato (ver ContratoFinanceiroService.desvincularImovel)
// e nasce/morre só na criação/exclusão, nunca é editado (vínculo é fixado na criação do contrato).
@Entity
@Table(name = "contrato_imovel", uniqueConstraints = @UniqueConstraint(columnNames = {"contrato_id", "imovel_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ContratoImovelModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contrato_id")
    private ContratoFinanceiroModel contrato;

    @ManyToOne(optional = false)
    @JoinColumn(name = "imovel_id")
    private ImovelModel imovel;

    @Column(name = "valor_alocado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorAlocado;
}
