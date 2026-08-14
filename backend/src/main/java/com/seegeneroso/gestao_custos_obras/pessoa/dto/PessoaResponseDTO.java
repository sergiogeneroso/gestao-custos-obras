package com.seegeneroso.gestao_custos_obras.pessoa.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoPessoa;

public record PessoaResponseDTO(
        Long id,
        String nome,
        TipoPessoa tipoPessoa,
        String documento,
        String email,
        String telefone,
        Boolean ativo
) {}
