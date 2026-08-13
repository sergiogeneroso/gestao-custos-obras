package com.seegeneroso.gestao_custos_obras.imovel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImovelFotoRepository extends JpaRepository<ImovelFotoModel, Long> {

    List<ImovelFotoModel> findByImovelId(Long imovelId);

    Optional<ImovelFotoModel> findByImovelIdAndPrincipalTrue(Long imovelId);

    List<ImovelFotoModel> findByImovelIdInAndPrincipalTrue(List<Long> imovelIds);
}
