package com.seegeneroso.gestao_custos_obras.shared.exclusao;

import com.seegeneroso.gestao_custos_obras.auth.UsuarioModel;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Exclusão lógica compartilhada (ADR-040): reaproveitada em todo domínio que precisa de
// "excluir", em vez de cada entidade reinventar ativo/motivo/quem/quando com nomes próprios.
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExclusaoLogica {

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "motivo_exclusao", columnDefinition = "TEXT")
    private String motivoExclusao;

    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;

    @ManyToOne
    @JoinColumn(name = "excluido_por_id")
    private UsuarioModel excluidoPor;

    public void excluir(String motivo, UsuarioModel usuario) {
        this.ativo = false;
        this.motivoExclusao = motivo;
        this.excluidoEm = LocalDateTime.now();
        this.excluidoPor = usuario;
    }
}
