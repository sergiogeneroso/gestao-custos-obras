package com.seegeneroso.gestao_custos_obras.imovel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// O cadastro pede só o que é do lote (ADR-033); construcao e casa vêm junto apenas na edição de
// um imóvel que já alcançou aquela fase, e o service ignora os grupos de fases futuras.
public record ImovelRequestDTO(

        @NotBlank(message = "Identificador é obrigatório")
        @Size(max = 50, message = "Identificador deve ter no máximo 50 caracteres")
        String identificador,

        @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres")
        String endereco,

        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres")
        String numero,

        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
        String bairro,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String cidade,

        @Size(max = 2, message = "UF deve ter 2 letras")
        String uf,

        @Pattern(regexp = "^$|^\\d{5}-?\\d{3}$", message = "CEP deve ter 8 dígitos")
        String cep,

        String observacaoEndereco,

        @Valid DadosLoteDTO lote,
        @Valid DadosConstrucaoDTO construcao,
        @Valid DadosCasaDTO casa,

        BigDecimal compraValor,

        @NotNull(message = "Data da compra é obrigatória")
        LocalDate compraData,

        Long compraVendedorId,
        Boolean compraParcelada,

        BigDecimal vendaValorPretendido,

        String descricao
) {}
