package com.seegeneroso.gestao_custos_obras.imovel.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// Usado tanto no cadastro/edição quanto no PATCH /fase para CONSTRUCAO, para a lista de campos
// da fase existir num lugar só. responsavelTecnicoNome só é preenchido na resposta.
public record DadosConstrucaoDTO(
        BigDecimal area,
        LocalDate dataInicio,
        LocalDate previsaoConclusao,
        BigDecimal custoEstimado,

        @Size(max = 50, message = "Número do alvará deve ter no máximo 50 caracteres")
        String alvaraNumero,

        LocalDate alvaraEmissao,
        LocalDate alvaraValidade,

        @Size(max = 50, message = "Número da ART deve ter no máximo 50 caracteres")
        String artNumero,

        Long responsavelTecnicoId,
        String responsavelTecnicoNome,

        @Size(max = 50, message = "CNO deve ter no máximo 50 caracteres")
        String cno
) {}
