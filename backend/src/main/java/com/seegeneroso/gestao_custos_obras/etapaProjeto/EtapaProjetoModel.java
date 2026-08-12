package com.seegeneroso.gestao_custos_obras.etapaProjeto;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "etapa_projeto")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EtapaProjetoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;
}
