package com.seegeneroso.gestao_custos_obras.shared.auditoria;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Um evento de auditoria (ADR-042): quem mudou o quê, quando, e o estado do registro antes/depois
 * em JSON — o snapshot é o ResponseDTO que o domínio já expõe pela API, nunca a entidade JPA crua.
 * Tabela genérica compartilhada por todo domínio de negócio, em vez de uma tabela de histórico por
 * domínio (ver .agents/rules/auditoria.md).
 */
@Entity
@Table(name = "log_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoriaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entidade;

    @Column(name = "entidade_id", nullable = false)
    private Long entidadeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperacaoAuditoria operacao;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private UsuarioModel usuario;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    // Nulo em CRIACAO (não havia estado anterior).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "estado_anterior", columnDefinition = "jsonb")
    private String estadoAnterior;

    // Nulo em EXCLUSAO física (CategoriaDespesa) — nas demais, reflete o registro já com
    // exclusao.ativo=false.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "estado_novo", columnDefinition = "jsonb")
    private String estadoNovo;
}
