package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoDocumentoImovel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "imovel_documento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImovelDocumentoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "imovel_id")
    private ImovelModel imovel;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumentoImovel tipoDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase_imovel", nullable = false, length = 20)
    private FaseImovel faseImovel;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "data_upload", nullable = false)
    @Builder.Default
    private LocalDateTime dataUpload = LocalDateTime.now();
}
