package com.seegeneroso.gestao_custos_obras.imovel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Usado tanto no cadastro/edição quanto no PATCH /fase para CONSTRUCAO, para a lista de campos
// da fase existir num lugar só. responsavelTecnicoNome só é preenchido na resposta.
public record DadosConstrucaoDTO(
        BigDecimal area,
        LocalDate dataInicio,
        LocalDate previsaoConclusao,
        BigDecimal custoEstimado,
        String alvaraNumero,
        LocalDate alvaraEmissao,
        LocalDate alvaraValidade,
        String artNumero,
        Long responsavelTecnicoId,
        String responsavelTecnicoNome,
        String cno
) {}
