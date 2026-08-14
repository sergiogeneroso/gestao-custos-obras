package com.seegeneroso.gestao_custos_obras.contratoFinanceiro;

import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import com.seegeneroso.gestao_custos_obras.shared.enums.SituacaoContrato;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoContratoFinanceiro;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contrato_financeiro")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ContratoFinanceiroModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "imovel_id")
    private ImovelModel imovel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoContratoFinanceiro tipo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contraparte_id")
    private PessoaModel contraparte;

    @Column(name = "valor_contratado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorContratado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SituacaoContrato situacao = SituacaoContrato.ATIVO;

    @Column(name = "data_quitacao")
    private LocalDate dataQuitacao;

    @Column(name = "valor_quitacao", precision = 14, scale = 2)
    private BigDecimal valorQuitacao;

    @Builder.Default
    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParcelaContratoModel> parcelas = new ArrayList<>();
}
