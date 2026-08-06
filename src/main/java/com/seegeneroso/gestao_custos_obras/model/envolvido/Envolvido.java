package com.seegeneroso.gestao_custos_obras.model.envolvido;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "envolvido")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Envolvido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    private String documento;
    private String email;
    private String telefone;

    @Column(name = "tipo_participacao", nullable = false, length = 50)
    private String tipoParticipacao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}