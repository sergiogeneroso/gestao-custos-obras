package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.shared.enums.FaseImovel;
import com.seegeneroso.gestao_custos_obras.shared.enums.TipoDocumentoImovel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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

    // Nome com que o arquivo foi enviado — o armazenado é gerado pelo StorageService, e sem isto
    // a lista mostraria só a URL.
    @Column(name = "nome_arquivo", length = 255)
    private String nomeArquivo;

    private String descricao;

    @Column(name = "data_emissao")
    private LocalDate dataEmissao;

    // Alvará vence, IPTU é anual: a validade é o que permite avisar antes de travar a obra.
    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Column(name = "data_upload", nullable = false)
    @Builder.Default
    private LocalDateTime dataUpload = LocalDateTime.now();
}
