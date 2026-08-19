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

    // Papel de fornecedor é uma marca na própria pessoa (substitui o domínio Fornecedor): a mesma
    // pessoa jurídica pode vender o lote numa operação e fornecer material em outra.
    // O DEFAULT no DDL não é decorativo: sem ele o ddl-auto=update não consegue adicionar uma
    // coluna NOT NULL a uma tabela que já tem linhas.
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean fornecedor = false;

    @Column(name = "area_atuacao")
    private String areaAtuacao;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
