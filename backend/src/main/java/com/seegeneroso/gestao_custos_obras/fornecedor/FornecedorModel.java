package com.seegeneroso.gestao_custos_obras.fornecedor;

import com.seegeneroso.gestao_custos_obras.pessoa.PessoaModel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fornecedor")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FornecedorModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "pessoa_id", unique = true)
    private PessoaModel pessoa;

    private String areaAtuacao;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
