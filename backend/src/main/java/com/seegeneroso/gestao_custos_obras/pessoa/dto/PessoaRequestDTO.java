package com.seegeneroso.gestao_custos_obras.pessoa.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoPessoa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PessoaRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotNull(message = "Tipo de pessoa é obrigatório")
        TipoPessoa tipoPessoa,

        @NotBlank(message = "Documento é obrigatório")
        String documento,

        String email,
        String telefone
) {}
