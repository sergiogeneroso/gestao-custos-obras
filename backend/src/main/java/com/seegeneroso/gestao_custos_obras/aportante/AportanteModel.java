package com.seegeneroso.gestao_custos_obras.aportante;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "aportante")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AportanteModel {

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