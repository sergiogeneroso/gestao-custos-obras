package com.seegeneroso.gestao_custos_obras.pessoa.dto;

import com.seegeneroso.gestao_custos_obras.shared.enums.TipoPessoa;
import com.seegeneroso.gestao_custos_obras.shared.validacao.Documentos;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PessoaRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        @NotNull(message = "Tipo de pessoa é obrigatório")
        TipoPessoa tipoPessoa,

        @NotBlank(message = "Documento é obrigatório")
        @Size(max = 20, message = "Documento deve ter no máximo 20 caracteres")
        String documento,

        @Email(message = "E-mail inválido")
        @Size(max = 255, message = "E-mail deve ter no máximo 255 caracteres")
        String email,

        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String telefone,

        Boolean fornecedor,

        @Size(max = 255, message = "Área de atuação deve ter no máximo 255 caracteres")
        String areaAtuacao,

        // observacoes é TEXT no banco: texto livre, sem teto.
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
