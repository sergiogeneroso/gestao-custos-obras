package com.seegeneroso.gestao_custos_obras.perfil;

import com.seegeneroso.gestao_custos_obras.shared.enums.AcaoPermissao;
import com.seegeneroso.gestao_custos_obras.shared.enums.DominioSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerfilPermissaoRepository extends JpaRepository<PerfilPermissaoModel, Long> {

    List<PerfilPermissaoModel> findByPerfilId(Long perfilId);

    // Só concede se o perfil ainda estiver ativo — um perfil excluído não autoriza mais nada,
    // mesmo que algum usuário ainda aponte pra ele (ADR-046).
    @Query("""
            select case when count(pp) > 0 then true else false end
            from PerfilPermissaoModel pp
            where pp.perfil.id = :perfilId
              and pp.perfil.exclusao.ativo = true
              and pp.dominio = :dominio
              and pp.acao = :acao
            """)
    boolean possuiPermissao(@Param("perfilId") Long perfilId,
                             @Param("dominio") DominioSistema dominio,
                             @Param("acao") AcaoPermissao acao);
}
