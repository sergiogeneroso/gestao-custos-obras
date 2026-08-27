package com.seegeneroso.gestao_custos_obras.pessoa.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoPessoa;
import com.seegeneroso.gestao_custos_obras.shared.validacao.Documentos;
import jakarta.validation.constraints.AssertTrue;
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
        String telefone,
        Boolean fornecedor,
        String areaAtuacao,
        String observacoes
) {

    /**
     * O documento só pode ser validado junto do tipo de pessoa, então a checagem vive aqui,
     * no record que tem os dois campos, em vez de virar uma annotation de campo. Campo vazio
     * ou tipo ausente passa de propósito: quem reclama disso é o @NotBlank/@NotNull acima, e
     * duas mensagens para a mesma omissão só confundiriam.
     */
    @AssertTrue(message = "Documento inválido para o tipo de pessoa informado")
    public boolean isDocumentoValido() {
        if (tipoPessoa == null || documento == null || documento.isBlank()) {
            return true;
        }
        return tipoPessoa == TipoPessoa.FISICA
                ? Documentos.cpfValido(documento)
                : Documentos.cnpjValido(documento);
    }
}
