package com.seegeneroso.gestao_custos_obras.imovel;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoDocumentoImovel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImovelDocumentoRepository extends JpaRepository<ImovelDocumentoModel, Long> {

    List<ImovelDocumentoModel> findByImovelId(Long imovelId);

    List<ImovelDocumentoModel> findByImovelIdAndTipoDocumento(Long imovelId, TipoDocumentoImovel tipoDocumento);
}
