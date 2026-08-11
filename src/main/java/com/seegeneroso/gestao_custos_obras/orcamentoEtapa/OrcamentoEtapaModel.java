package com.seegeneroso.gestao_custos_obras.orcamentoEtapa;

import com.seegeneroso.gestao_custos_obras.etapaProjeto.EtapaProjetoModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "orcamento_etapa",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orcamento_imovel_etapa", columnNames = {"imovel_id", "etapa_projeto_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrcamentoEtapaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "imovel_id")
    private ImovelModel imovel;

    @ManyToOne(optional = false)
    @JoinColumn(name = "etapa_projeto_id")
    private EtapaProjetoModel etapaProjeto;

    @Column(name = "valor_orcado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorOrcado;

    @Column(name = "data_inicio_prevista")
    private LocalDate dataInicioPrevista;

    @Column(name = "data_fim_prevista")
    private LocalDate dataFimPrevista;
}
