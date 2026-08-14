package com.seegeneroso.gestao_custos_obras.pessoa;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoPessoa;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pessoa")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PessoaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 20)
    private TipoPessoa tipoPessoa;

    @Column(nullable = false, unique = true)
    private String documento;

    private String email;
    private String telefone;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
