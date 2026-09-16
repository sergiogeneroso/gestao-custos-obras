package com.seegeneroso.gestao_custos_obras.orcamentoCategoria;

import com.seegeneroso.gestao_custos_obras.categoriaDespesa.CategoriaDespesaModel;
import com.seegeneroso.gestao_custos_obras.imovel.ImovelModel;
import com.seegeneroso.gestao_custos_obras.shared.exclusao.ExclusaoLogica;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "orcamento_categoria",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orcamento_imovel_categoria", columnNames = {"imovel_id", "categoria_despesa_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrcamentoCategoriaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "imovel_id")
    private ImovelModel imovel;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_despesa_id")
    private CategoriaDespesaModel categoriaDespesa;

    @Column(name = "valor_orcado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorOrcado;

    @Column(name = "data_inicio_prevista")
    private LocalDate dataInicioPrevista;

    @Column(name = "data_fim_prevista")
    private LocalDate dataFimPrevista;

    // columnDefinition com DEFAULT TRUE: ver o mesmo comentário em ContratoFinanceiroModel.
    @Embedded
    @AttributeOverride(name = "ativo", column = @Column(name = "ativo", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE"))
    @Builder.Default
    private ExclusaoLogica exclusao = new ExclusaoLogica();

    // Getter manual: mesmo motivo do de ImovelModel — @ManyToOne dentro do embeddable
    // (excluidoPor) faz o Hibernate devolver null em vez do objeto vazio.
    public ExclusaoLogica getExclusao() {
        if (exclusao == null) {
            exclusao = new ExclusaoLogica();
        }
        return exclusao;
    }
}
