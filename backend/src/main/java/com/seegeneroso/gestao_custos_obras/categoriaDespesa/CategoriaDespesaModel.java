package com.seegeneroso.gestao_custos_obras.categoriaDespesa;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categoria_despesa")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CategoriaDespesaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;
}
