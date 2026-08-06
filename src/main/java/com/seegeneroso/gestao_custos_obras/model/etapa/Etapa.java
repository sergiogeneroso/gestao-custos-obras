package com.seegeneroso.gestao_custos_obras.model.etapa;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "etapa")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Etapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;
}
