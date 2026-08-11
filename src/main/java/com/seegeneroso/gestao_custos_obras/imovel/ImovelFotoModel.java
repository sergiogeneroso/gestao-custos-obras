package com.seegeneroso.gestao_custos_obras.imovel;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "imovel_foto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImovelFotoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "imovel_id")
    private ImovelModel imovel;

    @Column(nullable = false, length = 500)
    private String url;

    private String legenda;

    @Column(name = "data_upload", nullable = false)
    @Builder.Default
    private LocalDateTime dataUpload = LocalDateTime.now();
}
