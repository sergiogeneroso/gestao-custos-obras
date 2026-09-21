package com.seegeneroso.gestao_custos_obras.perfil;

import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;
import jakarta.persistence.*;
import lombok.*;

// Uma linha da matriz perfil×domínio×ação (ADR-046): a existência da linha É a concessão, sem
// booleano à parte. Sem ExclusaoLogica própria — na edição do perfil a coleção inteira é
// substituída (clear + re-add), e o histórico de quem mudou o quê já fica no log de auditoria
// do Perfil (estadoAnterior/estadoNovo), não precisa de soft delete por linha.
@Entity
@Table(name = "perfil_permissao",
        uniqueConstraints = @UniqueConstraint(columnNames = {"perfil_id", "dominio", "acao"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PerfilPermissaoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "perfil_id")
    private PerfilModel perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DominioSistema dominio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcaoPermissao acao;
}
