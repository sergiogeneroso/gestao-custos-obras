package com.seegeneroso.gestao_custos_obras.despesa;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoAnexoDespesa;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "despesa_anexo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespesaAnexoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "despesa_id")
    private DespesaModel despesa;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_anexo", nullable = false, length = 20)
    private TipoAnexoDespesa tipoAnexo;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "data_upload", nullable = false)
    @Builder.Default
    private LocalDateTime dataUpload = LocalDateTime.now();
}
