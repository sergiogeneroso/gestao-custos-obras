package com.seegeneroso.gestao_custos_obras.perfil;

import com.seegeneroso.gestao_custos_obras.shared.exclusao.ExclusaoLogica;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "perfil")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PerfilModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Builder.Default
    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerfilPermissaoModel> permissoes = new ArrayList<>();

    @Embedded
    @Builder.Default
    private ExclusaoLogica exclusao = new ExclusaoLogica();

    // Getter manual: @ManyToOne dentro do embeddable (excluidoPor) faz o Hibernate devolver
    // null em vez do objeto vazio — mesmo padrão de ImovelModel/PessoaModel.
    public ExclusaoLogica getExclusao() {
        if (exclusao == null) {
            exclusao = new ExclusaoLogica();
        }
        return exclusao;
    }
}
