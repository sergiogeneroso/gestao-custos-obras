package com.seegeneroso.gestao_custos_obras.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<PerfilModel, Long> {

    @Query("select p from PerfilModel p where p.exclusao.ativo = true")
    List<PerfilModel> findByAtivoTrue();

    @Query("select p from PerfilModel p where p.id = :id and p.exclusao.ativo = true")
    Optional<PerfilModel> findByIdAndAtivoTrue(@Param("id") Long id);

    Optional<PerfilModel> findByNome(String nome);

    boolean existsByNome(String nome);
}
