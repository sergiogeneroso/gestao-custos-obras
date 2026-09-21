package com.seegeneroso.gestao_custos_obras.auth;

import com.seegeneroso.gestao_custos_obras.perfil.PerfilModel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UsuarioModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    // Substitui o antigo campo `role` (String solta) pela matriz configurável do RBAC (ADR-046).
    // Nullable: PerfilSeedRunner garante que todo usuário existente seja migrado pro perfil
    // "Administrador" na subida da aplicação.
    @ManyToOne
    @JoinColumn(name = "perfil_id")
    private PerfilModel perfil;
}
